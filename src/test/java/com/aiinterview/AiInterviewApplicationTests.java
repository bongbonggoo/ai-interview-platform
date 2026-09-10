package com.aiinterview;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 스캐폴딩이 Maven Central 없는 샌드박스에서 작성돼 한 번도 기동 검증이 안 됐었다.
 * 이 테스트가 JPA 매핑/빈 배선이 깨졌을 때 가장 먼저 잡아준다.
 */
@SpringBootTest
class AiInterviewApplicationTests {

    @Test
    void contextLoads() {
    }
}
