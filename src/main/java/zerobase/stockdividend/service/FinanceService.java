package zerobase.stockdividend.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import zerobase.stockdividend.exception.impl.NoCompanyException;
import zerobase.stockdividend.model.Company;
import zerobase.stockdividend.model.Dividend;
import zerobase.stockdividend.model.ScrapedResult;
import zerobase.stockdividend.model.constants.CacheKey;
import zerobase.stockdividend.persist.CompanyRepository;
import zerobase.stockdividend.persist.DividendRepository;
import zerobase.stockdividend.persist.entity.CompanyEntity;
import zerobase.stockdividend.persist.entity.DividendEntity;

import java.util.List;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
@Slf4j
public class FinanceService {

    private final CompanyRepository companyRepository;
    private final DividendRepository dividendRepository;

    // 요청이 얼마나 자주 들어오는지?
    // 자주 변경되는 데이터인지?

    @Cacheable(key = "#companyName", value = CacheKey.KEY_FINANCE)
    public ScrapedResult getDividendByCompanyName(String companyName) {
        log.info("search company -> " + companyName);
        //1. 회사명 기준으로 회사 정보 조회
        CompanyEntity company = this.companyRepository.findByName(companyName)
                .orElseThrow(() -> new NoCompanyException());

        //2. 조회된 회사 id로 배당금 정보 조회
        List<DividendEntity> dividendEntities = this.dividendRepository
                .findAllByCompanyId(company.getId());

        //3. 결과 조합 후 반환
//        List<Dividend> dividends = new ArrayList<>();
//        for (var entity : dividendEntities) {
//            dividends.add(Dividend.builder()
//                    .date(entity.getDate())
//                    .dividend(entity.getDividend())
//                    .build());
//        }

        List<Dividend> dividends = dividendEntities.stream()
                        .map(e -> new Dividend(e.getDate(), e.getDividend()))
                .collect(Collectors.toList());

        return new ScrapedResult(new Company(
                company.getTicker(), company.getName()),
                dividends);
    }
}
