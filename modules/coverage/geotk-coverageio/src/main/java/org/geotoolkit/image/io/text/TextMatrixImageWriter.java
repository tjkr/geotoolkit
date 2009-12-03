/*
 *    Geotoolkit.org - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2007-2009, Open Source Geospatial Foundation (OSGeo)
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

import java.io.BufferedWriter;
import java.io.IOException;
import java.text.FieldPosition;
import java.text.NumberFormat;
import java.util.Locale;
import javax.imageio.IIOImage;
import javax.imageio.ImageWriter;
import javax.imageio.ImageWriteParam;
import javax.imageio.spi.ImageWriterSpi;
import javax.imageio.metadata.IIOMetadata;
import javax.media.jai.iterator.RectIter;

import org.geotoolkit.util.Version;
import org.geotoolkit.util.Utilities;
import org.geotoolkit.image.ImageDimension;
import org.geotoolkit.resources.Descriptions;


/**
 * An image encoder for matrix of floating-point numbers. The sample values are formatted by an
 * {@link NumberFormat} instance. Consequently the format is locale-dependant, unless the locale
 * is explicitly specified either in the <cite>Service Provider Interface</cite> ({@link Spi#locale})
 * or by overriding the {@link #getDataLocale(ImageWriteParam)} method.
 * <p>
 * This format is typically used for writing one-banded image. However if an image with more than
 * one band is to be written, then each band is separated by a blank line in the output stream.
 *
 * @author Martin Desruisseaux (IRD)
 * @version 3.00
 *
 * @see TextMatrixImageReader
 *
 * @since 1.2
 * @module
 */
public class TextMatrixImageWriter extends TextImageWriter {
    /**
     * Amount of spaces to put between columns.
     */
    private static final int SEPARATOR_WIDTH = 1;

    /**
     * Constructs a new image writer.
     *
     * @param provider the provider that is invoking this constructor, or {@code null} if none.
     */
    protected TextMatrixImageWriter(final ImageWriterSpi provider) {
        super(provider);
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
        final BufferedWriter   out = getWriter(parameters);
        final String lineSeparator = getLineSeparator(parameters);
        final NumberFormat  format = createNumberFormat(image, parameters);
        final FieldPosition    pos = getExpectedFractionPosition(format);
        final int    fractionWidth = pos.getEndIndex() - pos.getBeginIndex();
        final int            width = pos.getEndIndex() + SEPARATOR_WIDTH;
        final StringBuffer  buffer = new StringBuffer(width);
        final RectIter    iterator = createRectIter(image, parameters);
        final ImageDimension  size = computeSize(image, parameters);
        final float  progressScale = 100f / size.getNumSampleValues();
        int numSampleValues = 0, nextProgress = 0;
        boolean moreBands = false;
        if (!iterator.finishedBands()) do {
            if (moreBands) {
                out.write(lineSeparator); // Separate bands by a blank line.
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
                if (!iterator.finishedPixels()) do {
                    buffer.setLength(0);
                    String n = format.format(iterator.getSampleDouble(), buffer, pos).toString();
                    final int fractionOffset = Math.max(0, fractionWidth - (pos.getEndIndex() - pos.getBeginIndex()));
                    out.write(Utilities.spaces(width - n.length() - fractionOffset));
                    out.write(n);
                    out.write(Utilities.spaces(fractionOffset));
                } while (!iterator.nextPixelDone());
                out.write(lineSeparator);
                numSampleValues += size.width;
                iterator.startPixels();
            } while (!iterator.nextLineDone());
            iterator.startLines();
            moreBands = true;
        } while (!iterator.nextBandDone());
        out.flush();
        processImageComplete();
    }




    /**
     * Service provider interface (SPI) for {@link TextMatrixImageWriter}s. This SPI provides
     * necessary implementation for creating default {@link TextMatrixImageWriter} using default
     * locale and character set. The {@linkplain #Spi default constructor} initializes the fields
     * to the values listed below:
     * <p>
     * <table border="1" cellspacing="0">
     *   <tr bgcolor="lightblue"><th>Field</th><th>Value</th></tr>
     *   <tr><td>&nbsp;{@link #names}           &nbsp;</td><td>&nbsp;{@code "matrix"}&nbsp;</td></tr>
     *   <tr><td>&nbsp;{@link #MIMETypes}       &nbsp;</td><td>&nbsp;{@code "text/plain"}&nbsp;</td></tr>
     *   <tr><td>&nbsp;{@link #pluginClassName} &nbsp;</td><td>&nbsp;{@code "org.geotoolkit.image.io.text.TextMatrixImageWriter"}&nbsp;</td></tr>
     *   <tr><td>&nbsp;{@link #vendorName}      &nbsp;</td><td>&nbsp;{@code "Geotoolkit.org"}&nbsp;</td></tr>
     *   <tr><td>&nbsp;{@link #version}         &nbsp;</td><td>&nbsp;{@link Version#GEOTOOLKIT}&nbsp;</td></tr>
     *   <tr><td colspan="2" align="center">See {@linkplain TextImageReader.Spi super-class javadoc} for remaining fields</td></tr>
     * </table>
     *
     * @author Martin Desruisseaux (IRD)
     * @version 3.00
     *
     * @see TextMatrixImageReader.Spi
     *
     * @since 2.4
     * @module
     */
    public static class Spi extends TextImageWriter.Spi {
        /**
         * Constructs a default {@code TextMatrixImageWriter.Spi}. The fields are initialized as
         * documented in the <a href="#skip-navbar_top">class javadoc</a>. Subclasses can modify
         * those values if desired.
         * <p>
         * For efficienty reasons, the above fields are initialized to shared arrays. Subclasses
         * can assign new arrays, but should not modify the default array content.
         */
        public Spi() {
            names           = TextMatrixImageReader.Spi.NAMES;
            MIMETypes       = TextMatrixImageReader.Spi.MIME_TYPES;
            pluginClassName = "org.geotoolkit.image.io.text.TextMatrixImageWriter";
            vendorName      = "Geotoolkit.org";
            version         = Version.GEOTOOLKIT.toString();
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
            return Descriptions.getResources(locale).getString(Descriptions.Keys.CODEC_MATRIX);
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
            return new TextMatrixImageWriter(this);
        }
    }
}
