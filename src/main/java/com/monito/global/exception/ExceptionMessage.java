package com.monito.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
//도메인이 늘어나면 각각 도메인에 같은 클래스들을 만들어 사용
public enum ExceptionMessage {

    // 인증 관련
    AUTHENTICATION_FAILED("이메일 또는 비밀번호가 올바르지 않습니다."),
    INVALID_PRINCIPAL_TYPE("유효하지 않은 인증입니다"),
    AUTHENTICATION_MISSING("인증에 실패했습니다."),
    LOGIN_FAILED("로그인에 실패했습니다."),
    TOKEN_REFRESH_FAILED("토큰 갱신에 실패했습니다."),

    // 토큰 관련
    INVALID_TOKEN("유효하지 않은 토큰입니다."),
    EXPIRED_TOKEN("만료된 토큰입니다."),
    REFRESH_TOKEN_NOT_FOUND("리프레시 토큰을 찾을 수 없습니다."),

    // 회원 관련
    MEMBER_NOT_FOUND("회원을 찾을 수 없습니다."),
    EMAIL_ALREADY_EXISTS("이미 사용 중인 이메일입니다."),
    NICKNAME_ALREADY_EXISTS("이미 사용 중인 닉네임입니다."),
    PHONE_ALREADY_EXISTS("이미 사용 중인 핸드폰번호입니다."),
    BUSINESSLICENSENUMBER_ALREADY_EXISTS("이미 사용 중인 사업자번호입니다."),
    INVALID_PASSWORD_FORMAT("비밀번호는 8자 이상, 영문/숫자/특수문자를 포함해야 합니다."),
    INCORRECT_PASSWORD("기존 비밀번호가 틀립니다 다시 입력해주세요"),

    // Member CRUD 관련 추가
    DUPLICATE_ACCOUNT_ID("이미 사용 중인 계정 ID입니다."),
    MEMBER_CREATE_FAILED("회원 등록에 실패했습니다."),
    MEMBER_UPDATE_FAILED("회원 정보 수정에 실패했습니다."),
    MEMBER_DELETE_FAILED("회원 삭제에 실패했습니다."),

    // 권한 관련
    ACCESS_DENIED("접근 권한이 없습니다."),
    ACCESS_DENIED_NOT_AUTHOR("해당 데이터의 작성자가 아닙니다."),
    INSUFFICIENT_PERMISSION("해당 작업을 수행할 권한이 없습니다."),

    // 일반 오류
    INTERNAL_SERVER_ERROR("서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요."),
    INVALID_REQUEST("잘못된 요청입니다."),
    RESOURCE_NOT_FOUND("요청한 리소스를 찾을 수 없습니다."),

    // 파일 관련
    FILE_NOT_SELECTED("업로드할 파일을 선택해주세요."),
    FILE_PROCESSING_ERROR("파일 처리 중 오류가 발생했습니다."),

    // 게시물 관련
    DATA_NOT_FOUND("데이터 정보가 없음"),
    CART_DATA_DEFERENCE("다른 가게 장바구니 데이터 존재"),
    STORE_NOT_FOUND("가게 데이터 정보가 없음"),
    MENU_NOT_FOUND("메뉴 데이터 정보가 없음"),
    CART_NOT_FOUND("카트 데이터 정보가 없음"),

    // 알림 관련
    GET_FCM_ACCESS_TOKEN_ERROR("FCM ACCESS TOKEN 조회중 오류 발생했습니다."),
    FCM_MESSAGE_JSON_PARSING_ERROR("FCM 메세지 JSON 변환중 오류 발생했습니다."),
    SEND_FCM_PUSH_ERROR("FCM 메세지 전송중 오류 발생했습니다."),

    // Alert 도메인 관련
    ALERT_NOT_FOUND("알림을 찾을 수 없습니다."),
    ALERT_READ_ACCESS_DENIED("본인의 알림만 읽음 처리할 수 있습니다."),
    ALERT_VIEW_ACCESS_DENIED("본인의 알림만 조회할 수 있습니다."),
    ALERT_DELETE_ACCESS_DENIED("본인의 알림만 삭제할 수 있습니다."),

    // AlertRule 도메인 관련
    ALERT_RULE_NOT_FOUND("알림 규칙을 찾을 수 없습니다."),
    ALERT_RULE_ALREADY_EXISTS("해당 컨테이너의 메트릭에 대한 알림 규칙이 이미 존재합니다."),
    ALERT_RULE_VIEW_ACCESS_DENIED("본인의 알림 규칙만 조회할 수 있습니다."),
    ALERT_RULE_UPDATE_ACCESS_DENIED("본인의 알림 규칙만 수정할 수 있습니다."),
    ALERT_RULE_DELETE_ACCESS_DENIED("본인의 알림 규칙만 삭제할 수 있습니다."),

    // 이메일 관련
    EMAIL_BAD_REQUEST("잘못된 이메일 요청입니다."),

    // 작업 로그 관련
    ISSUE_LOG_NOT_FOUND("작업 로그 데이터를 찾을 수 없습니다."),

    // Agent 관련
    AGENT_NOT_MATCH_PASSWORD("agentkey에 할당된 비밀번호가 일치하지 않습니다."),
    AGENT_NOT_FOUND("Agent를 찾을 수 없습니다."),
    AGENT_KEY_REQUIRED("Agent Key가 필요합니다."),

    // Container 관련
    CONTAINER_NOT_FOUND("컨테이너를 찾을 수 없습니다."),
    CONTAINER_HASH_INVALID("유효하지 않은 컨테이너 해시입니다."),
    CONTAINER_METRICS_PROCESSING_FAILED("컨테이너 메트릭 처리 중 오류가 발생했습니다."),
    CONTAINER_LOGS_PROCESSING_FAILED("컨테이너 로그 처리 중 오류가 발생했습니다."),
    CONTAINER_STATS_LOG_NOT_FOUND("컨테이너 통계 로그를 찾을 수 없습니다."),

    // Favorite 관련
    FAVORITE_NOT_FOUND("즐겨찾기를 찾을 수 없습니다."),
    FAVORITE_ALREADY_EXISTS("이미 즐겨찾기에 등록된 컨테이너입니다."),

    // 입력값 검증
    INVALID_INPUT_VALUE("입력값이 유효하지 않습니다.");

    private final String message;
}
