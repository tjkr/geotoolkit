/*
 *    Geotoolkit.org - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2009, Open Source Geospatial Foundation (OSGeo)
 *    (C) 2009, Geomatys
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.geotoolkit.image.io.text;

import java.awt.geom.AffineTransform;
import java.awt.image.DataBuffer;
import java.util.Locale;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.LinkedHashMap;
import javax.imageio.IIOImage;
import javax.imageio.IIOException;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.spi.ImageWriterSpi;
import javax.media.jai.iterator.RectIter;

import org.opengis.coverage.grid.RectifiedGrid;
import org.opengis.metadata.spatial.Georectified;
import org.opengis.metadata.spatial.PixelOrientation;

import org.geotoolkit.util.Version;
import org.geotoolkit.util.Utilities;
import org.geotoolkit.image.ImageDimension;
import org.geotoolkit.image.io.metadata.MetadataHelper;
import org.geotoolkit.image.io.metadata.SpatialMetadata;
import org.geotoolkit.resources.Errors;


/**
 * Writer for the ASCII Grid format. As the "ASCII" name implies, the data file are written in
 * US-ASCII character encoding no matter what the {@link Spi#charset} value is. In addition, the
 * US locale is enforced no matter what the {@link Spi#locale} value is.
 * <p>
 * This format writes only the standard header attributes defined in the ASCII Grid standard.
 * The Geotk extensions described in the {@link AsciiGridReader} class are not formatted by
 * default.
 *
 * @author Martin Desruisseaux (Geomatys)
 * @version 3.07
 *
 * @see <a href="http://daac.ornl.gov/MODIS/ASCII_Grid_Format_Description.html">ASCII Grid Format Description</a>
 * @see AsciiGridReader
 *
 * @since 3.07
 * @module
 */
public class AsciiGridWriter extends TextImageWriter {
    /**
     * Constructs a new image writer.
     *
     * @param provider the provider that is invoking this constructor, or {@code null} if none.
     */
    protected AsciiGridWriter(final ImageWriterSpi provider) {
        super(provider);
    }

    /**
     * Fills the given {@code header} map with values extracted from the given image metadata.
     * The {@code "NCOLS"} and {@code "NROWS"} attributes are already defined when this method
     * is invoked. This method is responsible for filling the remaining attributes.
     *
     * @param  metadata The metadata.
     * @param  header The map in which to store the (<var>key</var>, <var>value</var>) pairs
     *         to be written.
     * @throws IOException If the metadata can not be prepared.
     */
    private void prepareHeader(final SpatialMetadata metadata, final Map<String,String> header,
            final ImageWriteParam param) throws IOException
    {
        final MetadataHelper   helper    = new MetadataHelper(this);
        final Georectified     spatialRp = metadata.getInstanceForType(Georectified .class);
        final RectifiedGrid    domain    = metadata.getInstanceForType(RectifiedGrid.class);
        final PixelOrientation ptInPixel = spatialRp.getPointInPixel();
        final AffineTransform  gridToCRS = helper.getAffineTransform(domain, param);
        String xll = "XLLCORNER";
        String yll = "YLLCORNER";
        // Test UPPER_LEFT corder, not LOWER_LEFT, because the Y axis has been
        // reverted (i.e. the corresponding value in OffsetVectors is negative).
        if (ptInPixel != null && !ptInPixel.equals(PixelOrientation.UPPER_LEFT)) {
            if (ptInPixel.equals(PixelOrientation.CENTER)) {
                xll = "XLLCENTER";
                yll = "YLLCENTER";
            } else if (ptInPixel.equals(PixelOrientation.valueOf("UPPER"))) {
                yll = "YLLCENTER";
            } else if (ptInPixel.equals(PixelOrientation.valueOf("LEFT"))) {
                xll = "XLLCENTER";
            } else {
                throw new IIOException(error(Errors.Keys.BAD_PARAMETER_$2, "pointInPixel", ptInPixel));
            }
        }
        header.put(xll, String.valueOf(gridToCRS.getTranslateX()));
        header.put(yll, String.valueOf(gridToCRS.getTranslateY()));
        header.put("CELLSIZE", String.valueOf(helper.getCellSize(gridToCRS)));
    }

    /**
     * Invoked by the {@link #write write} method for appending the header to the output
     * stream. Subclasses can override this method in order to modify the header content.
     * The {@code header} map given in argument can be freely modified.
     *
     * @param  header The content of the header to be written.
     * @param  out The streal where to write the header.
     * @throws IOException If an error occured while writing the header.
     *
     * @todo Overriding not yet allowed. We are waiting to see if this API is really appropriate.
     */
    private void writeHeader(final Map<String,String> header, final BufferedWriter out) throws IOException {
        int length = 0;
        for (final String key : header.keySet()) {
            final int lg = key.length();
            if (lg > length) {
                length = lg;
            }
        }
        boolean first = true; // Do not write the line separator for the first line.
        for (final Map.Entry<String,String> entry : header.entrySet()) {
            if (!first) {
                out.write('\n');
            }
            first = false;
            final String key = entry.getKey();
            out.write(key);
            out.write(Utilities.spaces(1 + Math.max(0, length - key.length())));
            out.write(entry.getValue());
        }
        // We intentionaly ommit the line separator for the last line,
        // because the write(...) method below will add it itself.
    }

    /**
     * Appends a complete image stream containing a single image.
     *
     * @param  streamMetadata The stream metadata (ignored in default implementation).
     * @param  image The image or raster to be written.
     * @param  parameters The write parameters, or null if the whole image will be written.
     * @throws IOException If an error occured while writting to the stream.
     */
    @Override
    public void write(final IIOMetadata streamMetadata, final IIOImage image,
                      final ImageWriteParam parameters) throws IOException
    {
        processImageStarted();
        final BufferedWriter out  = getWriter(parameters);
        final ImageDimension size = computeSize(image, parameters);
        /*
         * Write the header.
         */
        final Map<String,String> header = new LinkedHashMap<String,String>(8);
        header.put("NCOLS", String.valueOf(size.width ));
        header.put("NROWS", String.valueOf(size.height));
        final SpatialMetadata metadata = convertImageMetadata(image.getMetadata(), null, parameters);
        if (metadata != null) {
            prepareHeader(metadata, header, parameters);
        }
        writeHeader(header, out);
        /*
         * Write the pixel values.
         */
        final RectIter iterator      = createRectIter(image, parameters);
        final int      dataType      = getSampleModel(image, parameters).getDataType();
        final float    progressScale = 100f / size.getNumSampleValues();
        int numSampleValues = 0, nextProgress = 0;
        boolean moreBands = false;
        if (!iterator.finishedBands()) do {
            if (moreBands) {
                out.write('\n'); // Separate bands by a blank line.
            }
            if (!iterator.finishedLines()) do {
                if (numSampleValues >= nextProgress) {
                    // Informs about progress only every 2000 numbers.
                    processImageProgress(progressScale * numSampleValues);
                    nextProgress = numSampleValues + 2000;
                }
                if (abortRequested()) {
                    processWriteAborted();
                    return;
                }
                char separator = '\n';
                if (!iterator.finishedPixels()) do {
                    final String value;
                    switch (dataType) {
                        case DataBuffer.TYPE_DOUBLE: {
                            value = Double.toString(iterator.getSampleDouble());
                            break;
                        }
                        case DataBuffer.TYPE_FLOAT: {
                            value = Float.toString(iterator.getSampleFloat());
                            break;
                        }
                        default: {
                            value = Integer.toString(iterator.getSample());
                            break;
                        }
                        case DataBuffer.TYPE_USHORT:
                        case DataBuffer.TYPE_BYTE: {
                            value = Integer.toString(iterator.getSample() & 0x7FFFFFFF);
                            break;
                        }
                    }
                    out.write(separator);
                    out.write(value);
                    separator = ' ';
                } while (!iterator.nextPixelDone());
                numSampleValues += size.width;
                iterator.startPixels();
            } while (!iterator.nextLineDone());
            iterator.startLines();
            moreBands = true;
        } while (!iterator.nextBandDone());
        out.write('\n');
        out.flush();
        processImageComplete();
    }




    /**
     * Service provider interface (SPI) for {@link AsciiGridWriter}s. This SPI provides
     * the necessary implementation for creating default {@link AsciiGriWriter}s using
     * US locale and ASCII character set. The {@linkplain #locale locale} and
     * {@linkplain #charset charset} fields are ignored by the default implementation.
     * <p>
     * The {@linkplain #Spi default constructor} initializes the fields to the values listed
     * below. Users wanting different values should create a subclass of {@code Spi} and set
     * the desired values in their constructor.
     * <p>
     * <table border="1" cellspacing="0">
     *   <tr bgcolor="lightblue"><th>Field</th><th>Value</th></tr>
     *   <tr><td>&nbsp;{@link #names}           &nbsp;</td><td>&nbsp;{@code "ascii-grid"}&nbsp;</td></tr>
     *   <tr><td>&nbsp;{@link #MIMETypes}       &nbsp;</td><td>&nbsp;{@code "text/plain"}&nbsp;</td></tr>
     *   <tr><td>&nbsp;{@link #pluginClassName} &nbsp;</td><td>&nbsp;{@code "org.geotoolkit.image.io.text.AsciiGridWriter"}&nbsp;</td></tr>
     *   <tr><td>&nbsp;{@link #vendorName}      &nbsp;</td><td>&nbsp;{@code "Geotoolkit.org"}&nbsp;</td></tr>
     *   <tr><td>&nbsp;{@link #version}         &nbsp;</td><td>&nbsp;{@link Version#GEOTOOLKIT}&nbsp;</td></tr>
     *   <tr><td>&nbsp;{@link #locale}          &nbsp;</td><td>&nbsp;{@link Locale#US}&nbsp;</td></tr>
     *   <tr><td>&nbsp;{@link #charset}         &nbsp;</td><td>&nbsp;{@code "US-ASCII"}&nbsp;</td></tr>
     *   <tr><td>&nbsp;{@link #lineSeparator}   &nbsp;</td><td>&nbsp;{@code "\n"}&nbsp;</td></tr>
     *   <tr><td colspan="2" align="center">See {@linkplain TextImageReader.Spi super-class javadoc} for remaining fields</td></tr>
     * </table>
     *
     * @author Martin Desruisseaux (Geomatys)
     * @version 3.07
     *
     * @see AsciiGridReader.Spi
     *
     * @since 3.07
     * @module
     */
    public static class Spi extends TextImageWriter.Spi {
        /**
         * Constructs a default {@code AsciiGridWriter.Spi}. The fields are initialized as
         * documented in the <a href="#skip-navbar_top">class javadoc</a>. Subclasses can
         * modify those values if desired.
         * <p>
         * For efficienty reasons, the above fields are initialized to shared arrays.
         * Subclasses can assign new arrays, but should not modify the default array content.
         */
        public Spi() {
            names           = AsciiGridReader.Spi.NAMES;
            MIMETypes       = AsciiGridReader.Spi.MIME_TYPES;
            pluginClassName = "org.geotoolkit.image.io.text.AsciiGridWriter";
            vendorName      = "Geotoolkit.org";
            version         = Version.GEOTOOLKIT.toString();
            locale          = Locale.US;
            charset         = Charset.forName("US-ASCII");
            lineSeparator   = "\n";
        }

        /**
         * Returns a brief, human-readable description of this service provider
         * and its associated implementation. The resulting string should be
         * localized for the supplied locale, if possible.
         *
         * @param  locale A Locale for which the return value should be localized.
         * @return A String containing a description of this service provider.
         */
        @Override
        public String getDescription(final Locale locale) {
            return "ASCII grid";
        }

        /**
         * Returns an instance of the {@code ImageWriter} implementation associated
         * with this service provider.
         *
         * @param  extension An optional extension object, which may be null.
         * @return An image writer instance.
         * @throws IOException if the attempt to instantiate the writer fails.
         */
        @Override
        public ImageWriter createWriterInstance(final Object extension) throws IOException {
            return new AsciiGridWriter(this);
        }
    }
}
