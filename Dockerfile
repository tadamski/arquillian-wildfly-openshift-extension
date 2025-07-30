ARG runtime_image=quay.io/wildfly/wildfly-runtime:latest

FROM ${runtime_image}

ARG server_dir
COPY --chown=jboss:root ${server_dir} $JBOSS_HOME
RUN chmod -R ug+rwX $JBOSS_HOME
