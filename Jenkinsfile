import org.jenkinsci.plugins.pipeline.modeldefinition.Utils

credentialID_GematikGitRepo = 'tst_tt_build_u_p'

def setGitCredentials() {
    //workaround da checkout die Credentials nicht lädt
    withCredentials([[$class          : 'UsernamePasswordMultiBinding', credentialsId: credentialID_GematikGitRepo,
                      usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD']]) {
        sh label: 'configure git credentials', script: """
        git config --global credential.username $USERNAME
        git config --global credential.helper "!echo password=$PASSWORD; echo"
        """
    }
}


node('Docker-Maven') {

    MAVEN_OPTS = " -e -ntp"
    String repoURL = 'https://build.top.local/source/git/idp/aforeporter'

    def linuxEnv = ['PATH=/opt/maven/apache-maven-3.6.3/bin:/usr/local/openjdk-11/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin']

    properties([
            disableConcurrentBuilds(),
            pipelineTriggers(
                    //   [pollSCM(
                    //       scmpoll_spec: 'H/5 * * * *',    // Check SCM (e.g. Git) all 5 minutes
                    //       ignorePostCommitHooks: true)
                    //   ]
            ),
            buildDiscarder(
                    logRotator(
                            artifactDaysToKeepStr: '',
                            artifactNumToKeepStr: '',
                            daysToKeepStr: '',
                            numToKeepStr: '5'
                    )
            ),
            parameters([
                    string(name: 'RCTag', defaultValue: '', description: 'Release Candidate Tag. Leave blank if snapshot from heads should be created.'),
                    booleanParam(name: 'Checkout', defaultValue: true, description: 'Checkout'),
                    booleanParam(name: 'Compile', defaultValue: true, description: 'Clean build and compile'),
                    booleanParam(name: 'Package', defaultValue: true, description: 'Package fat and annotation jar'),
                    booleanParam(name: 'Install', defaultValue: true, description: 'Create documentation and update Readme'),
                    booleanParam(name: 'Deploy', defaultValue: false, description: 'Deploy to annotation and fat jar to Nexus'),
                    booleanParam(name: 'Notification', defaultValue: true, description: 'Send notification via email'),
            ])
    ])

    echo "Params passed in:" + params.toString()

    // TODO remove
    String RCTag = params.containsKey('RCTag') ? params.RCTag : ''
    String ReleaseTag = RCTag - ~/-RC.*/

    boolean Checkout = params.containsKey('Checkout') ? params.Checkout : true
    boolean Compile = params.containsKey('Compile') ? params.Package : true
    boolean Package = params.containsKey('Package') ? params.Package : true
    boolean Install = params.containsKey('Install') ? params.Install : true
    boolean Deploy = params.containsKey('Deploy') ? params.Deploy : true
    boolean Notify = params.containsKey('Notification') ? params.Notification : true

    currentBuild.description = RCTag.isEmpty() ? 'SNAPSHOT' : RCTag + ' --> ' + ReleaseTag

    try {
        withEnv(linuxEnv) {
            stage('Checkout') {
                if (!Checkout) {
                    Utils.markStageSkippedForConditional(STAGE_NAME); return
                }
                cleanWs() //Wipe out repository & force clone
                setGitCredentials()
                checkout(
                        $class: 'GitSCM',
                        branches: [[name: '*/master']],
                        browser: [$class : 'GitLab',
                                  repoUrl: repoURL],
                        doGenerateSubmoduleConfigurations: false,
                        extensions: [
                                [$class: 'DisableRemotePoll'],
                                [$class             : 'SubmoduleOption',
                                 disableSubmodules  : true,
                                 parentCredentials  : false,
                                 recursiveSubmodules: true,
                                 reference          : '',
                                 trackingSubmodules : true]
                        ],
                        submoduleCfg: [],
                        userRemoteConfigs: [
                                [credentialsId: credentialID_GematikGitRepo,
                                 url          : repoURL + '.git']
                        ]
                )
            }

            stage('Compile') {
                if (!Compile) {
                    Utils.markStageSkippedForConditional(STAGE_NAME); return
                }
                sh """mvn clean compile ${MAVEN_OPTS} """
            }
            stage('Package') {
                if (!Package) {
                    Utils.markStageSkippedForConditional(STAGE_NAME); return
                }
                sh """mvn package ${MAVEN_OPTS}"""
            }
            stage('Install') {
                if (!Install) {
                    Utils.markStageSkippedForConditional(STAGE_NAME); return
                }
                sh """mvn install ${MAVEN_OPTS}"""
            }
            stage('Deploy') {
                if (!Deploy) {
                    Utils.markStageSkippedForConditional(STAGE_NAME); return
                }
                def alternative = RCTag.isEmpty() ? ' -DaltDeploymentRepository=snapshots::default::https://build.top.local/nexus/content/repositories/snapshots' : ' -DaltDeploymentRepository=releases::default::https://build.top.local/nexus/content/repositories/releases'
                def snapshots = ' -DaltSnapshotDeploymentRepository=snapshots::default::https://build.top.local/nexus/content/repositories/snapshots'
                def releases = ' -DaltReleaseDeploymentRepository=releases::default::https://build.top.local/nexus/content/repositories/releases'
                // full deploy because of shade plugin in submodules - otherwise the JAR is not the shaded JAR
                sh """mvn deploy  ${MAVEN_OPTS} ${alternative} ${snapshots} ${releases}"""
            }
        }

        stage('Notify via Email') {
            if (!Notify) {
                Utils.markStageSkippedForConditional(STAGE_NAME); return
            }

            //noinspection GroovyAssignabilityCheck
            emailext mimeType: 'text/html',
                    subject: '$DEFAULT_SUBJECT',
                    body: '${SCRIPT, template="groovy-html.template"}',
                    to: 'thomas.eitzenberger@gematik.de',
                    attachLog: true, compressLog: true
        }
    } catch (e) {
        echo "ERROR" + e.toString()
        currentBuild.result = 'FAILURE'
        if (Notify) {
            //noinspection GroovyAssignabilityCheck
            emailext mimeType: 'text/html',
                    subject: '$DEFAULT_SUBJECT' + ' - ' + e.toString(),
                    body: '${SCRIPT, template="groovy-html.template"}',
                    to: 'thomas.eitzenberger@gematik.de',
                    attachLog: true, compressLog: true
        }
    }

    if (!RCTag.isEmpty()) {
        currentBuild.setKeepLog(true)
    }
}