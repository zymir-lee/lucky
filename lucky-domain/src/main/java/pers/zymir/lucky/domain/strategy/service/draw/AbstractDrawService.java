package pers.zymir.lucky.domain.strategy.service.draw;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import pers.zymir.lucky.domain.strategy.model.dto.StrategyConfigDTO;
import pers.zymir.lucky.domain.strategy.model.req.DrawReq;
import pers.zymir.lucky.domain.strategy.model.res.DrawResult;
import pers.zymir.lucky.domain.strategy.repository.IStrategyRepository;
import pers.zymir.lucky.domain.strategy.service.algorithm.DrawAlgorithmCache;
import pers.zymir.lucky.domain.strategy.service.algorithm.DrawAlgorithmFactory;
import pers.zymir.lucky.domain.strategy.service.algorithm.IDrawAlgorithm;
import pers.zymir.lucky.exception.BusinessException;
import pers.zymir.lucky.exception.ResultCode;
import pers.zymir.lucky.po.Strategy;
import pers.zymir.lucky.po.StrategyDetail;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;

@Slf4j
public abstract class AbstractDrawService implements IDrawService {

    @Autowired
    private IStrategyRepository strategyRepository;

    @Override
    public DrawResult executeDraw(DrawReq drawReq) {

        // 1.查询策略信息 初始化奖品概率信息到内存当中
        Long strategyId = drawReq.getStrategyId();
        StrategyConfigDTO strategyConfigDTO = strategyRepository.queryStrategyConfig(strategyId);
        List<StrategyDetail> strategyDetails = strategyConfigDTO.getStrategyDetails();

        DrawAlgorithmCache.checkAndInitAwardRate(strategyId, strategyDetails);

        // 2.TODO 查询空库存奖品/风控


        // 3.执行抽奖算法
        Strategy strategy = strategyConfigDTO.getStrategy();
        IDrawAlgorithm drawAlgorithm = DrawAlgorithmFactory.getDrawAlgorithmByCode(strategy.getStrategyMode());
        if (Objects.isNull(drawAlgorithm)) {
            log.warn("执行抽奖算法失败，未找到对应算法，模式：{}", strategy.getStrategyMode());
            throw new BusinessException(ResultCode.DRAW_ALGORITHM_NOT_FOUND);
        }

        Long awardId = drawAlgorithm.randomDraw(strategyId, new HashSet<>());
        boolean isLucky = Objects.nonNull(awardId);

        // 4.封装结果
        DrawResult drawResult = new DrawResult();
        drawResult.setIsLucky(isLucky);
        drawResult.setAwardId(awardId);
        if (isLucky) {
            drawResult.setAwardName("");
        }

        return drawResult;
    }
}