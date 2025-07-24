PROJECT_DIR=$(pwd)

eval "$(crc oc-env)"

echo "creating project wildfly-testsuite"
oc login -u developer -p developer https://api.crc.testing:6443 > /dev/null
oc new-project wildfly-testsuite > /dev/null

echo "installing wildfly-operator"
oc login -u kubeadmin -p kubeadmin https://api.crc.testing:6443 > /dev/null
if [[ -z "$OPERATOR_DIR" ]]; then
    echo "Must provide the directory of wildfly-operator in OPERATOR_DIR variable" 1>&2
    exit 1
fi
cd "$OPERATOR_DIR" || exit
echo "installing operator"
make install >> /dev/null
make deploy >> /dev/null
oc adm policy add-role-to-user wildfly-operator developer --role-namespace=wildfly-testsuite -n wildfly-testsuite
echo "logging as developer"
oc login -u developer -p developer https://api.crc.testing:6443 > /dev/null
cd "$PROJECT_DIR" || exit

echo "adding haproxy helm chart"
helm repo add haproxytech https://haproxytech.github.io/helm-charts > /dev/null
helm repo update > /dev/null