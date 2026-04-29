import http from "k6/http";
import { check, sleep } from "k6";

const BASE_URL = __ENV.BASE_URL || "http://127.0.0.1:8080";
const USER_ID = __ENV.USER_ID || "1";
const USER_EMAIL = __ENV.USER_EMAIL || "loadtest@campusform.local";

export const options = {
  scenarios: {
    project_list: {
      executor: "constant-vus",
      vus: Number(__ENV.VUS || 10),
      duration: __ENV.DURATION || "20s",
    },
  },
};

export default function () {
  const url = `${BASE_URL}/api/projects`;
  const params = {
    headers: {
      "X-Loadtest-UserId": String(USER_ID),
      "X-Loadtest-Email": String(USER_EMAIL),
    },
  };

  const res = http.get(url, params);
  check(res, { "status is 200": (r) => r.status === 200 });
  sleep(0.1);
}

