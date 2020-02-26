FROM alfresco/alfresco-share:6.2.0

ARG TOMCAT_DIR=/usr/local/tomcat

# Remove old GoogleDocs AMP
RUN rm -f $TOMCAT_DIR/amps_share/alfresco-google*-share*.amp
RUN java -jar $TOMCAT_DIR/alfresco-mmt/alfresco-mmt-*.jar uninstall \
              org.alfresco.integrations.share.google.docs \
              $TOMCAT_DIR/webapps/share/


# Copy Dockerfile to avoid an error if no AMPs exist
COPY maven/target/*.amp $TOMCAT_DIR/amps_share/
RUN java -jar $TOMCAT_DIR/alfresco-mmt/alfresco-mmt*.jar install \
              $TOMCAT_DIR/amps_share $TOMCAT_DIR/webapps/share -directory -nobackup

