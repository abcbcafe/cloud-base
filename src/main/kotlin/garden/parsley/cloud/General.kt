package garden.parsley.cloud

import software.amazon.awscdk.App
import software.amazon.awscdk.Stack
import software.amazon.awscdk.services.ec2.*
import software.amazon.awscdk.services.ecs.Cluster
import software.amazon.awscdk.services.ecs.EfsVolumeConfiguration
import software.amazon.awscdk.services.efs.FileSystem
import software.amazon.awscdk.services.efs.LifecyclePolicy
import software.amazon.awscdk.services.iam.ManagedPolicy
import software.amazon.awscdk.services.iam.Role
import software.amazon.awscdk.services.iam.ServicePrincipal
import software.amazon.awscdk.services.logs.LogGroup
import software.amazon.awscdk.services.logs.RetentionDays

fun main() {
    val app = App.Builder.create().build()
    val stack = Stack.Builder.create(app, "General")
        .terminationProtection(true)
        .build()

    val vpc = Vpc.Builder.create(stack, "GeneralVpc")
        .subnetConfiguration(listOf(SubnetConfiguration.builder().name("Public")
            .subnetType(SubnetType.PUBLIC).build()))
        .build()

    val fargateLg = LogGroup.Builder.create(vpc, "GeneralFargateLogGroup").logGroupName("GeneralFargateLogGroup").retention(RetentionDays.FIVE_MONTHS).build()

    val efsSg = SecurityGroup.Builder.create(vpc, "efsSg")
        .vpc(vpc)
        .build()
    efsSg.addIngressRule(Peer.ipv4("10.0.0.0/8"), Port.tcp(2049))

    val fs = FileSystem.Builder.create(vpc, "GeneralStorage")
        .fileSystemName("General")
        .lifecyclePolicy(LifecyclePolicy.AFTER_7_DAYS)
        .vpc(vpc)
        .securityGroup(efsSg)
        .build()

    val role = Role.Builder.create(vpc, "SsmRole")
        .assumedBy(ServicePrincipal.Builder.create("ec2.amazonaws.com").build())
        .managedPolicies(listOf(ManagedPolicy.fromAwsManagedPolicyName("AmazonSSMManagedInstanceCore")))
        .build()

    val cluster = Cluster.Builder.create(vpc, "GeneralCluster")
        .clusterName("GeneralCluster")
        .enableFargateCapacityProviders(true)
        .vpc(vpc)
        .build()

    val efs = FileSystem.Builder.create(vpc, "Efs")
        .vpc(vpc)
        .lifecyclePolicy(LifecyclePolicy.AFTER_7_DAYS)
        .build()

    val efsConfig = EfsVolumeConfiguration.builder()
        .fileSystemId(efs.fileSystemId).build()

    println(app.synth().directory)
}

