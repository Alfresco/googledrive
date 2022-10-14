FROM quay.io/alfresco/alfresco-content-repository:7.3.0-A26

ARG IMAGEUSERNAME=alfresco
ARG TOMCAT_DIR=/usr/local/tomcat

# default user is alfresco (added on the base image alfresco/alfresco-content-repository:6.2.0)
# change to root user to be able to install the amps
USER root

# Remove old GoogleDocs AMP
RUN rm -f $TOMCAT_DIR/amps/alfresco-google*-repo*.amp
RUN java -jar $TOMCAT_DIR/alfresco-mmt/alfresco-mmt-*.jar uninstall \
              org.alfresco.integrations.google.docs \
              $TOMCAT_DIR/webapps/alfresco/

# Copy Dockerfile to avoid an error if no AMPs exist
COPY maven/target/*.amp $TOMCAT_DIR/amps/
RUN java -jar $TOMCAT_DIR/alfresco-mmt/alfresco-mmt*.jar install \
             $TOMCAT_DIR/amps/alfresco-google*-repo*.amp $TOMCAT_DIR/webapps/alfresco -nobackup

USER ${IMAGEUSERNAME}
