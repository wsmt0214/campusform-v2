import http from "k6/http";
import { check, sleep } from "k6";
import { Counter } from "k6/metrics";

const BASE_URL = __ENV.BASE_URL || "http://localhost:8080";
const PROJECT_ID = __ENV.PROJECT_ID;
const APPLICANT_ID = __ENV.APPLICANT_ID;
const USER_ID = __ENV.USER_ID;
const USER_EMAIL = __ENV.USER_EMAIL || "loadtest@campusform.local";

const status200 = new Counter("status_200");
const status409 = new Counter("status_409");
const status4xx = new Counter("status_4xx");
const status5xx = new Counter("status_5xx");
const statusOther = new Counter("status_other");

export const options = {
  scenarios: {
    concurrent_patch: {
      executor: "constant-vus",
      vus: Number(__ENV.VUS || 50),
      duration: __ENV.DURATION || "5s",
    },
  },
};

export default function () {
  if (!PROJECT_ID || !APPLICANT_ID || !USER_ID) {
    throw new Error("PROJECT_ID, APPLICANT_ID, USER_ID required");
  }

  const url = `${BASE_URL}/api/projects/${PROJECT_ID}/applicants/${APPLICANT_ID}?stage=DOCUMENT`;

  const statusCandidates = ["PASS", "FAIL", "HOLD"];
  const status = statusCandidates[Math.floor(Math.random() * statusCandidates.length)];

  const payload = JSON.stringify({ status });
  const params = {
    headers: {
      "Content-Type": "application/json",
      "X-Loadtest-UserId": String(USER_ID),
      "X-Loadtest-Email": String(USER_EMAIL),
    },
  };

  const res = http.patch(url, payload, params);

  if (res.status === 200) status200.add(1);
  else if (res.status === 409) status409.add(1);
  else if (res.status >= 400 && res.status < 500) status4xx.add(1);
  else if (res.status >= 500 && res.status < 600) status5xx.add(1);
  else statusOther.add(1);

  check(res, {
    "status is 200": (r) => r.status === 200,
    "status is 409": (r) => r.status === 409,
    "status is other 4xx": (r) =>
      r.status >= 400 && r.status < 500 && r.status !== 409,
    "status is 5xx": (r) => r.status >= 500 && r.status < 600,
  });

  sleep(0.1);
}

export function teardown() {
  if (!PROJECT_ID || !APPLICANT_ID || !USER_ID) {
    throw new Error("PROJECT_ID, APPLICANT_ID, USER_ID required");
  }

  const url = `${BASE_URL}/api/projects/${PROJECT_ID}/applicants/${APPLICANT_ID}?stage=DOCUMENT`;
  const payload = JSON.stringify({ status: "HOLD" });
  const params = {
    headers: {
      "Content-Type": "application/json",
      "X-Loadtest-UserId": String(USER_ID),
      "X-Loadtest-Email": String(USER_EMAIL),
    },
  };

  http.patch(url, payload, params);
}