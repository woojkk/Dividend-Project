package zerobase.stockdividend.web;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.*;
import zerobase.stockdividend.model.Company;
import zerobase.stockdividend.model.constants.CacheKey;
import zerobase.stockdividend.persist.entity.CompanyEntity;
import zerobase.stockdividend.service.CompanyService;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/company")
@AllArgsConstructor
@Slf4j
public class CompanyController {

    private final CompanyService companyService;
    private final CacheManager redisCacheManager;

    @GetMapping("/autocomplete")
    public ResponseEntity<?> autocomplete(@RequestParam String keyword) {
        var result = this.companyService.getCompanyNamesByKeyword(keyword);
        log.info("search keyword -> " + keyword);

        return ResponseEntity.ok(result.stream().limit(10).collect(Collectors.toList()));
    }

    @GetMapping
    @PreAuthorize("hasRole('READ')")
    public ResponseEntity<?> searchCompany(final Pageable pageable) {
        Page<CompanyEntity> companies = this.companyService.getAllCompany(pageable);
        log.info("all company search");

        return ResponseEntity.ok(companies);

    }

    @PostMapping
    @PreAuthorize("hasRole('WRITE')")
    public ResponseEntity<?> addCompany(@RequestBody Company request) {
        String ticker = request.getTicker().trim();
        if (ObjectUtils.isEmpty(ticker)) {
            throw new RuntimeException("ticker is empty");
        }

        try {
            Company company = this.companyService.save(ticker);
            this.companyService.addAutocompleteKeyword(company.getName());
            log.info("company add -> " + company.getName());
            return ResponseEntity.ok(company);
        } catch (IndexOutOfBoundsException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid ticker");
        }
    }

    @DeleteMapping("/{ticker}")
    @PreAuthorize("hasRole('WRITE')")
    public ResponseEntity<?> deleteCompany(@PathVariable String ticker) {
        String companyName = this.companyService.deleteCompany(ticker);
        this.clearFinanceCache(companyName);
        log.info("company delete -> " + companyName);

        return ResponseEntity.ok(companyName);
    }

    public void clearFinanceCache(String companyName) {
        Objects.requireNonNull(this.redisCacheManager.getCache(CacheKey.KEY_FINANCE))
                .evict(companyName);

    }
}
