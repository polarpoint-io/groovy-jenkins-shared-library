package io.polarpoint.utils


public void nodeTemplate(body) {

    def nodePodYaml = '''
apiVersion: v1
kind: Pod
metadata:
  generateName: agent-k8s-
  labels:
    name: nodejs
spec:
  dnsConfig:
    options:
    - name: ndots
      value: "1"
  resources:
      limits:
        cpu: "2"
        memory: '2048Mi'
      requests:
        cpu: "1"
        memory: 1024Mi'
  securityContext:
    runAsUser: 1000
    fsUser: 2000
    allowPrivilegeEscalation: false
  containers:
  - name: nodejs
    image: image.pohzn.com/pol-node:8.9.4
    tty: true
  imagePullPolicy: IfNotPresent 
  imagePullSecrets:
    - name: registry-credentials 
'''

    def label = "nodejs-${UUID.randomUUID().toString()}"
    podTemplate(
            label: label,
            yaml: nodePodYaml,
            inheritFrom: 'jenkins/jnlp-slave',
            nodeUsageMode: 'EXCLUSIVE',
            podRetention: onFailure(),
            idleMinutes: 1) {
        body.call(label)
    }
}



return this