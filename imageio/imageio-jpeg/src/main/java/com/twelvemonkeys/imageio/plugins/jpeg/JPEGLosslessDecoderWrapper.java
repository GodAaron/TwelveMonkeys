/*
 * Copyright (c) 2016, Harald Kuhr
 * Copyright (c) 2016, Herman Kroll
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name "TwelveMonkeys" nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.twelvemonkeys.imageio.plugins.jpeg;

import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferUShort;
import java.awt.image.Raster;
import java.io.IOException;
import java.util.List;

/**
 * This class provides the conversion of input data
 * containing a JPEG Lossless to an BufferedImage.
 * <p>
 * Take care, that only the following lossless formats are supported:
 * 1.2.840.10008.1.2.4.57 JPEG Lossless, Nonhierarchical (Processes 14)
 * 1.2.840.10008.1.2.4.70 JPEG Lossless, Nonhierarchical (Processes 14 [Selection 1])
 * <p>
 * Currently the following conversions are supported
 * - 24Bit, RGB       -> BufferedImage.TYPE_INT_RGB
 * -  8Bit, Grayscale -> BufferedImage.TYPE_BYTE_GRAY
 * - 16Bit, Grayscale -> BufferedImage.TYPE_USHORT_GRAY
 *
 * @author Hermann Kroll
 */
final class JPEGLosslessDecoderWrapper {

    /**
     * Decodes a JPEG Lossless stream to a {@code BufferedImage}.
     * Currently the following conversions are supported:
     * - 24Bit, RGB       -> BufferedImage.TYPE_3BYTE_BGR
     * -  8Bit, Grayscale -> BufferedImage.TYPE_BYTE_GRAY
     * - 16Bit, Grayscale -> BufferedImage.TYPE_USHORT_GRAY
     *
     * @param segments
     * @param input input stream which contains a jpeg lossless data
     * @return if successfully a BufferedImage is returned
     * @throws IOException is thrown if the decoder failed or a conversion is not supported
     */
    BufferedImage readImage(final List<Segment> segments, final ImageInputStream input) throws IOException {
        JPEGLosslessDecoder decoder = new JPEGLosslessDecoder(segments, input);

        int[][] decoded = decoder.decode();
        int width = decoder.getDimX();
        int height = decoder.getDimY();

        // Single component, assumed to be Gray
        if (decoder.getNumComponents() == 1) {
            switch (decoder.getPrecision()) {
                case 8:
                    return to8Bit1ComponentGrayScale(decoded, width, height);
                case 16:
                    return to16Bit1ComponentGrayScale(decoded, width, height);
                default:
                    throw new IOException("JPEG Lossless with " + decoder.getPrecision() + " bit precision and 1 component cannot be decoded");
            }
        }

        // 3 components, assumed to be RGB
        if (decoder.getNumComponents() == 3) {
            switch (decoder.getPrecision()) {
                case 8:
                    return to24Bit3ComponentRGB(decoded, width, height);

                default:
                    throw new IOException("JPEG Lossless with " + decoder.getPrecision() + " bit precision and 3 components cannot be decoded");
            }
        }

        throw new IOException("JPEG Lossless with " + decoder.getPrecision() + " bit precision and " + decoder.getNumComponents() + " component(s) cannot be decoded");
    }

    Raster readRaster(final List<Segment> segments, final ImageInputStream input) throws IOException {
        // TODO: Can perhaps be implemented faster
        return readImage(segments, input).getRaster();
    }

    /**
     * Converts the decoded buffer into a BufferedImage.
     * precision: 16 bit, componentCount = 1
     *
     * @param decoded data buffer
     * @param width   of the image
     * @param height  of the image
     * @return a BufferedImage.TYPE_USHORT_GRAY
     */
    private BufferedImage to16Bit1ComponentGrayScale(int[][] decoded, int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_USHORT_GRAY);
        short[] imageBuffer = ((DataBufferUShort) image.getRaster().getDataBuffer()).getData();

        for (int i = 0; i < imageBuffer.length; i++) {
            imageBuffer[i] = (short) decoded[0][i];
        }

        return image;
    }

    /**
     * Converts the decoded buffer into a BufferedImage.
     * precision: 8 bit, componentCount = 1
     *
     * @param decoded data buffer
     * @param width   of the image
     * @param height  of the image
     * @return a BufferedImage.TYPE_BYTE_GRAY
     */
    private BufferedImage to8Bit1ComponentGrayScale(int[][] decoded, int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        byte[] imageBuffer = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();

        for (int i = 0; i < imageBuffer.length; i++) {
            imageBuffer[i] = (byte) decoded[0][i];
        }

        return image;
    }

    /**
     * Converts the decoded buffer into a BufferedImage.
     * precision: 8 bit, componentCount = 3
     *
     * @param decoded data buffer
     * @param width   of the image
     * @param height  of the image
     * @return a BufferedImage.TYPE_3BYTE_RGB
     */
    private BufferedImage to24Bit3ComponentRGB(int[][] decoded, int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
        byte[] imageBuffer = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();

        for (int i = 0; i < imageBuffer.length / 3; i++) {
            // Convert to RGB (BGR)
            imageBuffer[i * 3 + 2] = (byte) decoded[0][i];
            imageBuffer[i * 3 + 1] = (byte) decoded[1][i];
            imageBuffer[i * 3] = (byte) decoded[2][i];
        }

        return image;
    }

}
