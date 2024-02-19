# TODO remove quay.io/ after share 6.2.1 image published to hub.docker.com/r/alfresco/alfresco-share
FROM quay.io/alfresco/alfresco-share:23.2.0-A18

ARG TOMCAT_DIR=/usr/local/tomcat

# Remove old GoogleDocs AMP
RUN rm -f $TOMCAT_DIR/amps_share/alfresco-google*-share*.amp
RUN java -jar $TOMCAT_DIR/alfresco-mmt/alfresco-mmt-*.jar uninstall \
              org.alfresco.integrations.share.google.docs \
              $TOMCAT_DIR/webapps/share/


# Copy Dockerfile to avoid an error if no AMPs exist
COPY maven/target/*.amp $TOMCAT_DIR/amps_share/
RUN java -jar $TOMCAT_DIR/alfresco-mmt/alfresco-mmt*.jar install \
              $TOMCAT_DIR/amps_share/alfresco-google*-share*.amp $TOMCAT_DIR/webapps/share -nobackup

