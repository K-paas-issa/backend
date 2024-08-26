package k_paas.balloon.keeper.infrastructure.client;

import static k_paas.balloon.keeper.global.constant.ClimateContants.ARRAY_X_INDEX;
import static k_paas.balloon.keeper.global.constant.ClimateContants.ARRAY_Y_INDEX;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
public class ClimateClientImpl implements ClimateClient {

    /* 기후 api url 문자열 생성 후, 통신과 결과 값 2차원 배열에 저장하는 함수 호출 */


    private final RestTemplate restTemplate;

    public ClimateClientImpl(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    //TODO: tmfc(api 업데이트 시간에 대한 변수) 추가로직 작성
    public String[][] sendRequestClimateData(String varn, String level, String predictHour) {
        StringBuilder urlBuilder = new StringBuilder("https://apihub.kma.go.kr/api/typ06/cgi-bin/url/nph-um_grib_xy_txt1");
        urlBuilder.append("?" + URLEncoder.encode("group", StandardCharsets.UTF_8) + "=" + URLEncoder.encode("UMKR", StandardCharsets.UTF_8)); /* 모델 구분(한반도 모델 -> 1.5km) */
        urlBuilder.append("&" + URLEncoder.encode("nwp", StandardCharsets.UTF_8) + "=" + URLEncoder.encode("N512", StandardCharsets.UTF_8)); /* 모델기반 종류 (여기선 상관x) */
        urlBuilder.append("&" + URLEncoder.encode("data", StandardCharsets.UTF_8) + "=" + URLEncoder.encode("P", StandardCharsets.UTF_8)); /* 자료 종류(P = 등압면) */
        urlBuilder.append("&" + URLEncoder.encode("varn", StandardCharsets.UTF_8) + "=" + URLEncoder.encode(varn, StandardCharsets.UTF_8)); /* 변수 종류 (grib 파일 참고) U, V 벡터를 위한 변수 */
        urlBuilder.append("&" + URLEncoder.encode("level", StandardCharsets.UTF_8) + "=" + URLEncoder.encode(level, StandardCharsets.UTF_8)); /* 고도 특정 고도만 출력 가능 (grib 파일 참고) */
        urlBuilder.append("&" + URLEncoder.encode("map", StandardCharsets.UTF_8) + "=" + URLEncoder.encode("F", StandardCharsets.UTF_8)); /* 사용 영역 (F -> 자료 전체 영역) */
        urlBuilder.append("&" + URLEncoder.encode("tmfc", StandardCharsets.UTF_8) + "=" + URLEncoder.encode("2024070900", StandardCharsets.UTF_8)); /* 분석 시간 */
        urlBuilder.append("&" + URLEncoder.encode("hf", StandardCharsets.UTF_8) + "=" + URLEncoder.encode(predictHour, StandardCharsets.UTF_8)); /* 예측 시간 (+00 hour)*/
        urlBuilder.append("&" + URLEncoder.encode("authKey", StandardCharsets.UTF_8) + "=" + URLEncoder.encode("KolWTlbHQaqJVk5WxwGqnw", StandardCharsets.UTF_8)); /* 발급된 API 인증키 */

        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-type", "application/json; text/plain; charset=utf-8");

        HttpEntity<?> entity = new HttpEntity<>(headers);
//        log.info("URL = [{}]", urlBuilder);
        ResponseEntity<String> response = restTemplate.exchange(
                urlBuilder.toString(),
                HttpMethod.GET,
                entity,
                String.class);

        String responseBody = response.getBody();
        String[][] result = this.parseResponseBody(responseBody);
        return result;
    }

    // TODO: 요청 데이터 가공 비효율적인 부분 수정 필요
    public String[][] parseResponseBody(String responseBody) {
//        log.info("문자열 파싱 시작 [{}] 쓰레드명", Thread.currentThread().getName());
//        log.info("responseBody = [{}]", responseBody);
        String[] lines = responseBody.split("\n");
        String[][] resStringArray = new String[ARRAY_Y_INDEX][ARRAY_X_INDEX];
        StringBuilder sb = new StringBuilder();
        int resArrayIndex = 0;
        boolean appendCondition = false;

        for(String line : lines) {
                /* "#j"로 시작하는 줄일 경우, 그 아래부터 값들이 나오기 때문에
                * 하나의 row에 해당하는 string을 StringBuilder로 연결한 것을 파싱하여 각각의 인덱스로 저장*/
            if (line.startsWith("#j")) {
                appendCondition = true;

                //가장 처음 #j 가 나올 경우, 아래 로직을 수행하면 안되므로
                if (sb.isEmpty()) {
                    continue;
                }
                resStringArray[resArrayIndex] = sb.toString().trim().split("\\s+");
                resArrayIndex++;
                sb.setLength(0);
                continue;
            }

            // "# "로 시작하는 라인일 경우, 문자열 배열 파싱과는 관련없는 문자열이므로
            if (line.startsWith("# ")) {
                appendCondition = false;
            }

            if (appendCondition) {
//                System.out.println("line = " + line);
                sb.append(line);
            }
        }
//        log.info("문자열 파싱 완료 쓰레드명 : [{}] ", Thread.currentThread().getName());
        return resStringArray;
    }
}
