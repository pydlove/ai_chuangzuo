package com.aichuangzuo.admin.modules.simulation.service;

import com.aichuangzuo.admin.modules.generation.service.AiCallResult;
import com.aichuangzuo.admin.modules.generation.service.GenerationAiService;
import com.aichuangzuo.admin.modules.modelconfig.entity.ModelConfig;
import com.aichuangzuo.admin.modules.modelconfig.mapper.ModelConfigMapper;
import com.aichuangzuo.shared.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SimulationProfileGeneratorTest {

    @Mock
    private GenerationAiService generationAiService;
    @Mock
    private ModelConfigMapper modelConfigMapper;

    @InjectMocks
    private SimulationProfileGenerator generator;

    @Test
    void generateNicknameReturnsCleanText() {
        ModelConfig cfg = new ModelConfig();
        cfg.setId(9L);
        cfg.setIsActive(1);
        when(modelConfigMapper.selectOne(any())).thenReturn(cfg);
        when(generationAiService.call(anyLong(), anyString(), anyString(), isNull(), anyBoolean()))
                .thenReturn(new AiCallResult("\"山茶星球\"\n", 1, 1, 2));

        String nickname = generator.generateNickname();

        assertEquals("山茶星球", nickname);
        verify(generationAiService).call(org.mockito.ArgumentMatchers.eq(9L), anyString(), anyString(),
                isNull(), org.mockito.ArgumentMatchers.eq(false));
    }

    @Test
    void generateBioUsesNicknameInPrompt() {
        ModelConfig cfg = new ModelConfig();
        cfg.setId(3L);
        cfg.setIsActive(1);
        when(modelConfigMapper.selectOne(any())).thenReturn(cfg);
        when(generationAiService.call(anyLong(), anyString(), anyString(), isNull(), anyBoolean()))
                .thenReturn(new AiCallResult("记录普通日子里的闪闪发光", 1, 1, 2));

        String bio = generator.generateBio("山茶星球");

        assertEquals("记录普通日子里的闪闪发光", bio);
        verify(generationAiService).call(anyLong(), anyString(),
                org.mockito.ArgumentMatchers.contains("山茶星球"), isNull(), anyBoolean());
    }

    @Test
    void throwsWhenNoActiveModelConfig() {
        when(modelConfigMapper.selectOne(any())).thenReturn(null);

        assertThrows(BusinessException.class, () -> generator.generateNickname());
    }

    @Test
    void throwsWhenContentBlank() {
        ModelConfig cfg = new ModelConfig();
        cfg.setId(1L);
        cfg.setIsActive(1);
        when(modelConfigMapper.selectOne(any())).thenReturn(cfg);
        when(generationAiService.call(anyLong(), anyString(), anyString(), isNull(), anyBoolean()))
                .thenReturn(new AiCallResult("   ", 1, 1, 2));

        assertThrows(BusinessException.class, () -> generator.generateBio("x"));
    }
}
