import http from 'k6/http';
import { check, sleep } from 'k6';

// 1. 테스트 옵션 설정 (가상 유저와 테스트 시간 정의)
export let options = {
  stages: [
    // 1단계: 10초 동안 0명에서 1000명까지 유저 수를 서서히 늘립니다. (Ramp-up)
    { duration: '10s', target: 1000 },
    
    // 2단계: 1000명의 유저가 30초 동안 계속해서 서버를 두드립니다. (Peak 부하 유지)
    { duration: '30s', target: 1000 },
    
    // 3단계: 마지막 10초 동안 유저 수를 다시 0으로 줄입니다. (Ramp-down)
    { duration: '10s', target: 0 },
  ],
};

// 2. 테스트 환경 변수 (Nginx 로드밸런서의 주소)
// 도커 환경에서 Nginx가 80 포트로 띄워져 있다고 가정합니다.
const BASE_URL = 'http://localhost';

// 3. 실제 가상 유저(VU)들이 반복해서 실행할 행동
export default function () {
  
  // 1만 명의 서로 다른 유저 ID 중 하나를 랜덤으로 뽑습니다. (예: user_4821)
  const randomUserId = 'user_' + Math.floor(Math.random() * 10000);

  // 헤더에 유저 ID를 담습니다.
  const params = {
    headers: {
      'Content-Type': 'application/json',
      'X-User-Id': randomUserId,
    },
  };

  // [행동 1] 대기열 진입 API (POST /api/v1/waiting) 를 호출합니다.
  // 이 API는 빈 바디(Body)를 보내도 되므로 빈 껍데기 '{}'를 보냅니다.
  let res = http.post(`${BASE_URL}/api/v1/waiting`, '{}', params);

  // [검증] 응답이 200 OK로 잘 왔는지(서버가 안 터졌는지) 확인합니다.
  check(res, {
    'is status 200': (r) => r.status === 200,
  });

  // 유저가 새로고침 버튼을 미친듯이 연타하지 않도록, 요청 후 1초(1000ms) 쉽니다.
  // 실제 사람의 행동과 비슷한 패턴을 만들기 위함입니다.
  sleep(1);
}
