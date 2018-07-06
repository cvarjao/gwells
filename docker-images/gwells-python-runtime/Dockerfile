FROM openshift/python:3.5
RUN source /opt/app-root/etc/scl_enable && \
    set -x && \
    pip install -U pip setuptools wheel && \
    mkdir -p ${APP_ROOT}/src/frontend && \
    curl -sLo ${APP_ROOT}/src/frontend/package.json ${NODE_PACKAGE_URL} && \
    curl -sLo ${APP_ROOT}/src/requirements.txt ${PYTHON_REQUIREMENTS_URL} && \
    sha256sum "${APP_ROOT}/src/requirements.txt" "${APP_ROOT}/src/frontend/package.json" > ${APP_ROOT}/dependencies.sha256  && \
    cd ${APP_ROOT}/src && pip install -r requirements.txt && \
    cd ${APP_ROOT}/src/frontend && npm install --loglevel info
    