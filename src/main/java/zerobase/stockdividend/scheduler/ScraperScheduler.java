package zerobase.stockdividend.scheduler;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import zerobase.stockdividend.model.Company;
import zerobase.stockdividend.model.ScrapedResult;
import zerobase.stockdividend.model.constants.CacheKey;
import zerobase.stockdividend.persist.CompanyRepository;
import zerobase.stockdividend.persist.DividendRepository;
import zerobase.stockdividend.persist.entity.CompanyEntity;
import zerobase.stockdividend.persist.entity.DividendEntity;
import zerobase.stockdividend.scraper.Scraper;

import java.util.List;

@Slf4j
@Component
@EnableCaching
@AllArgsConstructor
public class ScraperScheduler {

    private final CompanyRepository companyRepository;

    private final DividendRepository dividendRepository;
    private final Scraper yahooFinanceScraper;

    @CacheEvict(value = CacheKey.KEY_FINANCE, allEntries = true)
    @Scheduled(cron = "${scheduler.scrap.yahoo}")
    //스크래핑사이 3초 텀 있어서 크론주기로 데이터 가져오기 쉽지 않음
    //이때 스프링 배치 사용하면 대용량 처리하는데 유용
    public void yahooFinanceScheduling() {
        log.info("scraping scheduler is started");
        // 저장된 회사 목록 조회
        List<CompanyEntity> companies = companyRepository.findAll();
        // 회사마다 배당금 정보 새로 스크래핑
        for (var company : companies) {
            log.info("scraping scheduler is started ->" + company.getName());
            ScrapedResult scrapedResult = yahooFinanceScraper
                    .scrap(new Company(company.getTicker(), company.getName()));

            // 스프래핑한 배당금 정보 중 데이터베이스에 없는 값 저장
            scrapedResult.getDividendEntities().stream()
                    // dividend 모델을 dividendEntity 로 맵핑
                    .map(e -> new DividendEntity(company.getId(), e))
                    //엘리먼트를 하나씩 dividendRepository 에 삽입
                    .forEach(e -> {
                        boolean exists = dividendRepository.existsByCompanyIdAndDate(
                                e.getCompanyId(), e.getDate());
                        if (!exists) {
                            dividendRepository.save(e);
                            log.info("insert new dividend -> " + e.toString());
                        }
                    });
            // 연속적으로 스크래핑 대사 사이트 서버에 요청을 날리지 않도록 일시정지
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

        }
    }
}
