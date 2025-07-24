eval "$(crc oc-env)"

for release in $(helm ls --short)
do
  helm delete "$release"
done

for server in $(oc get wildflyservers.wildfly.org -o name)
do
  kubectl delete "$server"
done
