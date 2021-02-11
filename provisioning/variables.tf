
variable "aws_region" {
    type = string
    description = "The aws region where the infrastructure should be provisioned"
}
variable "environment" {
    type = string
    description = "The environment for the execution/setup"
}

variable "vpc_cidr_block" {
    type = string
    description = "The cidr block for at the VPC level"
}

variable "availability_zone_1" {
    type = string
    description = "The first availability zone where infrastructure should be provisioned"
}