package com.pawtrail.report;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * 실제 데이터베이스가 필요한 검사가 물려받습니다.
 *
 * 컨테이너를 직접 띄우고 내리지 않습니다.
 * 정적 블록에서 한 번 시작하면 JVM 이 끝날 때까지 그대로 두고,
 * 뒷정리는 Testcontainers 의 Ryuk 가 합니다.
 *
 * <b>@Container 를 쓰지 않는 이유가 여기 있습니다.</b>
 * 그 애너테이션은 컨테이너의 생명주기를 선언한 클래스에 묶습니다.
 * 검사 클래스가 하나일 때는 문제가 없으나 여럿이 물려받으면,
 * 먼저 끝난 클래스가 컨테이너를 내리고 뒤에 도는 클래스가 죽은 주소로 붙으려 합니다.
 * policy 에서 실제로 겪었고 그때 이 모양으로 바꿨습니다.
 *
 * 컨테이너가 한 번만 뜨므로 검사가 늘어도 느려지지 않습니다.
 * 스프링 컨텍스트도 설정이 같으면 캐시된 것을 다시 씁니다.
 *
 * 주소와 계정은 @DynamicPropertySource 가 넣습니다.
 * 컨테이너를 직접 관리하면 @ServiceConnection 이 붙을 자리가 없어집니다.
 * 그래서 테스트 설정 파일에는 여전히 spring.datasource 를 적지 않습니다.
 *
 * 이미지는 postgres:17-alpine 입니다.
 * 이 서비스는 좌표 타입을 쓰지 않아 PostGIS 가 필요 없고,
 * arm64 를 지원해 Apple Silicon 에서 에뮬레이션 없이 돕니다.
 * 부분 유일 인덱스의 NULLS NOT DISTINCT 가 PostgreSQL 15 부터라 운영과 같은 17 로 둡니다.
 *
 * import 가 org.testcontainers.postgresql 인 것에 주의합니다.
 * 제네릭이 붙은 옛 클래스는 org.testcontainers.containers 에 하위 호환용으로 남아
 * deprecated 이며, PostgreSQLContainer 뒤에 물음표가 붙어 있으면 그쪽을 쓰고 있다는 뜻입니다.
 */
@SpringBootTest
public abstract class IntegrationTestSupport {

    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }
}
