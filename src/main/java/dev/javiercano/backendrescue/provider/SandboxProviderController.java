package dev.javiercano.backendrescue.provider;

import java.util.UUID;
import org.springframework.web.bind.annotation.*;

// Local simulator only. No payment SDK, real credentials or real charges.
@RestController
@RequestMapping("/sandbox-provider")
public class SandboxProviderController {
    @PostMapping("/charges")
    public ProviderChargeResponse charge(@RequestBody ProviderChargeRequest request) {
        return new ProviderChargeResponse("sandbox-" + UUID.randomUUID());
    }
}
