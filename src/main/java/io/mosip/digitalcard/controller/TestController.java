package io.mosip.digitalcard.controller;

import io.mosip.digitalcard.service.PixelPassService;
import io.mosip.digitalcard.service.PrintInjiVcService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.mosip.digitalcard.util.DigitalCardRepoLogger;
import io.mosip.kernel.core.logger.spi.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.Map;

@RestController
@RequestMapping("/test")
public class TestController {

    private static final Logger logger = DigitalCardRepoLogger.getLogger(TestController.class);

    @Autowired
    private PrintInjiVcService printInjiVcService;

    @Autowired
    private PixelPassService pixelPassService;

    @PostMapping("/print")
    public String print(@RequestBody Map<String, Object> request) {
        String firstName = (String) request.get("firstName");
        String lastName = (String) request.get("lastName");
        String email = (String) request.get("email");
        String phone = (String) request.get("phone");

        System.out.println("==================================");
        System.out.println("First Name : " + firstName);
        System.out.println("Last Name  : " + lastName);
        System.out.println("Email      : " + email);
        System.out.println("Phone      : " + phone);
        System.out.println("==================================");

        String vc = printInjiVcService.generatePreAuthorizedCode(
                firstName,
                lastName,
                email,
                phone);

        String qr = pixelPassService.generateQRCode(vc);

        return "Success Gova";
    }
}