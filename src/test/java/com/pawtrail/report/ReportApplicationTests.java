package com.pawtrail.report;

import org.junit.jupiter.api.Test;

// 애플리케이션 컨텍스트가 뜨는지만 확인하는 검사임
// 본문이 비어 있어도 앱을 통째로 한 번 띄워보므로
// 빈 배선이 깨졌거나 자동 설정이 안 켜졌으면 여기서 드러남
//
// * 컨테이너 설정은 IntegrationTestSupport 에 있음
//   템플릿에서는 이 파일이 직접 들고 있었으나 데이터베이스를 쓰는 검사가 늘면서
//   한 곳으로 옮겼음.  클래스마다 정적 필드를 두면 컨테이너가 그만큼 뜸
//   @Container 가 아니라 정적 블록으로 띄우는 이유도 그 파일에 적어 두었음
//
// * 이 검사가 실제로 하는 일
//   엔티티와 스키마를 대조함.  ddl-auto 가 validate 라 컬럼이 어긋나면 기동이 막힘
//   Flyway 가 V1 · V2 · V20 을 돌고 그 결과를 엔티티가 그대로 읽을 수 있어야 함
class ReportApplicationTests extends IntegrationTestSupport {

    @Test
    void contextLoads() {
    }

}
