/*
 *    Geotoolkit - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2013, Geomatys
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
package org.geotoolkit.display3d.scene.loader;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.util.Collection;
import javax.measure.converter.ConversionException;

import org.apache.sis.util.ArgumentChecks;

import org.geotoolkit.image.interpolation.Interpolation;
import org.geotoolkit.image.interpolation.InterpolationCase;
import org.geotoolkit.image.interpolation.Resample;
import org.geotoolkit.image.iterator.PixelIterator;
import org.geotoolkit.image.iterator.PixelIteratorFactory;
import org.geotoolkit.referencing.CRS;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.internal.referencing.j2d.AffineTransform2D;
import org.geotoolkit.storage.coverage.AbstractGridMosaic;
import org.geotoolkit.storage.coverage.GridMosaic;
import org.geotoolkit.storage.coverage.GridMosaicRenderedImage;
import org.geotoolkit.storage.coverage.Pyramid;
import org.geotoolkit.storage.coverage.PyramidalCoverageReference;
import org.geotoolkit.display.PortrayalException;
import org.geotoolkit.display3d.utils.TextureUtils;

import org.opengis.geometry.Envelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.NoninvertibleTransformException;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.FactoryException;

/**
 * Generate tile images for terrain.
 *
 * @author Thomas Rouby (Geomatys)
 * @author Johann Sorel (Geomatys)
 */
public class PyramidImageLoader implements ImageLoader{

    private final PyramidalCoverageReference ref;
    private final Pyramid dataSource;
    private GridMosaicRenderedImage dataRenderedImage = null;

    private CoordinateReferenceSystem outputCrs;
    private MathTransform transformToOutput, transformFromOutput;

    public PyramidImageLoader(final PyramidalCoverageReference ref, final Pyramid dataSource) throws FactoryException, ConversionException {
        ArgumentChecks.ensureNonNull("pyramid", dataSource);
        this.ref = ref;
        this.dataSource = dataSource;
    }

    public PyramidalCoverageReference getCoverageReference() {
        return ref;
    }

    public void setOutputCRS(CoordinateReferenceSystem outputCrs) throws PortrayalException {
        this.outputCrs = outputCrs;
        try {
            createTransformOutput();
        } catch (FactoryException | ConversionException ex) {
            throw new PortrayalException(ex);
        }
    }

    /**
     * Internal only, use setOutputCRS to recalculate output transform
     * @throws FactoryException
     * @throws ConversionException
     */
    private void createTransformOutput() throws FactoryException, ConversionException {
        if (outputCrs != null){
            final CoordinateReferenceSystem crsImg = this.dataSource.getCoordinateReferenceSystem();
            transformToOutput = CRS.findMathTransform(crsImg, outputCrs, true);
            try {
                transformFromOutput = transformToOutput.inverse();
            } catch (NoninvertibleTransformException ex) {
                transformFromOutput = CRS.findMathTransform(outputCrs, crsImg, true);
            }
        }
    }

    @Override
    public BufferedImage getBufferedImageOf(Envelope outputEnv, Dimension outputDimension) throws PortrayalException {
        if(outputCrs == null){
            throw new PortrayalException("Output crs has not been set");
        }

        if (!CRS.equalsApproximatively(outputEnv.getCoordinateReferenceSystem(), outputCrs)){
            this.setOutputCRS(outputEnv.getCoordinateReferenceSystem());
        }

        final Envelope env;
        try {
            env = CRS.transform(transformFromOutput, outputEnv);
        } catch (TransformException ex) {
            throw new PortrayalException(ex);
        }

        final double scale = env.getSpan(0)/outputDimension.width;
        final int indexImg = TextureUtils.getNearestScaleIndex(dataSource.getScales(), scale);

        if (dataRenderedImage != null) {
            final GridMosaic gridMosaic = dataRenderedImage.getGridMosaic();
            final double mosaicScale = gridMosaic.getScale();
            final double mosaicIndex = TextureUtils.getNearestScaleIndex(dataSource.getScales(), mosaicScale);
            if (gridMosaic.getPyramid() != dataSource || mosaicIndex != indexImg) {
                final Collection<GridMosaic> mosaics = dataSource.getMosaics(indexImg);
                if (!mosaics.isEmpty()) {
                    dataRenderedImage = new GridMosaicRenderedImage(mosaics.iterator().next());
                } else {
                    dataRenderedImage = null;
                    return null;
                }
            }
        } else {
            final Collection<GridMosaic> mosaics = dataSource.getMosaics(indexImg);
            if (!mosaics.isEmpty()) {
                dataRenderedImage = new GridMosaicRenderedImage(mosaics.iterator().next());
            } else {
                dataRenderedImage = null;
                return null;
            }
        }
        try {
            return extractTileImage(outputEnv, dataRenderedImage, transformFromOutput, outputDimension);
        } catch (TransformException ex) {
            throw new PortrayalException(ex);
        }
    }

    private static BufferedImage extractTileImage(final Envelope tileEnvelope, final GridMosaicRenderedImage dataRenderedImage,
            final MathTransform transformFromOutput, final Dimension tileSize) throws TransformException {
        if (dataRenderedImage == null) {
            return null;
        }

        final double targetTileWidth = tileSize.width;
        final double targetTileHeight = tileSize.height;

        final GridMosaic gridmosaic = dataRenderedImage.getGridMosaic();
        final MathTransform mosaicCrsToMosaicGrid = AbstractGridMosaic.getTileGridToCRS(gridmosaic, new Point(0, 0)).inverse();

        final AffineTransform2D targetGridToTargetCrs = new AffineTransform2D(
                tileEnvelope.getSpan(0)/targetTileWidth,
                0, 0,
                -tileEnvelope.getSpan(1)/targetTileHeight,
                tileEnvelope.getMinimum(0),
                tileEnvelope.getMaximum(1));

        final ColorModel sourceColorModel = dataRenderedImage.getColorModel();
        final Raster prototype = dataRenderedImage.getData(new Rectangle(1, 1));
        //prepare the output image
        final WritableRaster targetRaster = prototype.createCompatibleWritableRaster(tileSize.width, tileSize.height);
        final BufferedImage targetImage = new BufferedImage(sourceColorModel, targetRaster, sourceColorModel.isAlphaPremultiplied(), null);

        final MathTransform sourceToTarget = MathTransforms.concatenate(
                targetGridToTargetCrs, transformFromOutput, mosaicCrsToMosaicGrid);

        //resample image
        final double[] fillValue = new double[targetImage.getData().getNumBands()];
        final PixelIterator it = PixelIteratorFactory.createRowMajorIterator(dataRenderedImage);
        final Interpolation interpol = Interpolation.create(it, InterpolationCase.NEIGHBOR, 2);
        final Resample resampler = new Resample(sourceToTarget, targetImage, interpol, fillValue);
        resampler.fillImage();

        return targetImage;
    }

}
