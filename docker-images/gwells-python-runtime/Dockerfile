FROM openshift/python:3.5
RUN source scl_source enable rh-python35 rh-nodejs6 && \
source ${APP_ROOT}/bin/activate && \
set -x && \
pip install -U pip setuptools wheel && \
mkdir -p ${APP_ROOT}/src/frontend && \
curl -sLo ${APP_ROOT}/src/frontend/package.json ${NODE_PACKAGE_URL} && \
curl -sLo ${APP_ROOT}/src/frontend/package-lock.json ${NODE_PACKAGE_LOCK_URL}