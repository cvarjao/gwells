FROM openshift/python:3.5
RUN source scl_source enable rh-python35 rh-nodejs6 && \
source ${APP_ROOT}/bin/activate && \
pip install -U pip setuptools wheel && \
curl -o ${APP_ROOT}/src/requirements.txt ${PYTHON_REQUIREMENTS_URL} && \
mkdir -p ${APP_ROOT}/src/frontend && \
curl -o ${APP_ROOT}/src/frontend/package-lock.json ${NODE_PACKAGES_URL} && \
(cd ${APP_ROOT}/src && pipenv install -r requirements.txt) && \
(cd ${APP_ROOT}/src/frontend && npm install)