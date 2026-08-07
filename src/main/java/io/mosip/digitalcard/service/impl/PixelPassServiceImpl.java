package io.mosip.digitalcard.service.impl;
import io.mosip.digitalcard.service.PixelPassService;
import io.mosip.pixelpass.PixelPass;
import io.mosip.pixelpass.types.ECC;
import org.springframework.stereotype.Service;
import io.mosip.digitalcard.util.DigitalCardRepoLogger;
import io.mosip.kernel.core.logger.spi.Logger;
@Service
public class PixelPassServiceImpl implements PixelPassService {

    private static final Logger logger = DigitalCardRepoLogger.getLogger(PixelPassServiceImpl.class);
    @Override
    public String generateQRCode(String vc) {

        try {

            logger.info("Starting PixelPass QR generation...");

            PixelPass pixelpass = new PixelPass();

            logger.info("PixelPass object created successfully.");

            String base64PngImage = pixelpass.generateQRCode(vc, ECC.H, "");

            logger.info("QR generated successfully.");



            String imageSrc =  base64PngImage;
            logger.info("QR Image Src : {}", imageSrc);
            return base64PngImage;

        } catch (Throwable e) {

            logger.error("Error while generating QR Code", e);
            e.printStackTrace();

            throw new RuntimeException("Failed to generate QR Code", e);
        }
    }
}
