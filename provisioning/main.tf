terraform {
  required_providers {
      aws = {
          source = "hashicorp/aws"
          version = "~> 3.0"
      }
  }
}

provider "aws" {
    region = var.aws_region
    profile = "clamp"
}

resource "aws_vpc" "clamp_vpc" {
    cidr_block  = var.vpc_cidr_block

    tags = {
        Name = "clamp_vpc"
        Team = "clamp"
        Environment = var.environment
    }
}

resource "tls_private_key" "clamp_ssh" {
    algorithm = "rsa"
    rsa_bits = 4096
}

resource "aws_key_pair" "clamp_ssh" {
    key_name = "ClampSSHKey"
    public_key = tls_private_key.clamp_ssh.public_key_openssh
}

output "clamp_ssh_private_key_pem" {
    value = tls_private_key.clamp_ssh.private_key_pem
}

output "clamp_ssh_public_key_pem" {
    value = tls_private_key.clamp_ssh.public_key_pem
}

resource "aws_subnet" "clamp_public_subnet" {
    vpc_id = aws_vpc.clamp_vpc.id
    cidr_block = "10.2.1.0/24"
    availability_zone = var.availability_zone_1


    tags = {
        Name = "clamp_public_subnet"
        Team = "clamp"
        Environment = var.environment
    }
}

resource "aws_subnet" "clamp_private_subnet_1" {
    vpc_id = aws_vpc.clamp_vpc.id
    cidr_block = "10.2.2.0/24"
    availability_zone = var.availability_zone_1


    tags = {
        Name = "clamp_private_subnet_1"
        Team = "clamp"
        Environment = var.environment
    }
}

resource "aws_security_group" "clamp_instance_access" {
    name = "ClampInstanceSecuriyyGroup"
    description = "Security group to control instance access for clamp instances"
    vpc_id = aws_vpc.clamp_vpc.id
    ingress {
        cidr_blocks = ["0.0.0.0/0"]
        from_port = 0
        to_port = 0
        protocol = "-1"
    }
    egress {
        cidr_blocks = ["0.0.0.0/0"]
        from_port = 0
        to_port = 0
        protocol = "-1"
    }

    tags = {
        Name = "clamp_instance_security_group"
        Team = "clamp"
        Environment = var.environment
    }
}

resource "aws_instance" "clamp_core_instance" {
    instance_type = "t2.micro"
    ami = ""
    subnet_id = aws_subnet.clamp_private_subnet_1.id
    security_groups = [aws_security_group.clamp_instance_access.id]
    key_name = aws_key_pair.clamp_ssh.key_name
    disable_api_termination = false
    ebs_optimized = false
    root_block_device {
        volume_size = "10"
    }

    tags = {
        Name = "clamp_core_instance"
        Team = "clamp"
        Environment = var.environment
    }
}

output "clamp_core_instance_ip" {
    value = aws_instance.clamp_core_instance.private_ip
}

resource "aws_internet_gateway" "clamp_ig" {
  vpc_id = aws_vpc.clamp_vpc.id

  tags = {
    Name = "clamp_ig"
    Team = "clamp"
    Environment = var.environment
  }
}

resource "aws_route_table" "clamp_public_subnet_route_table" {
    vpc_id = aws_vpc.clamp_vpc.id
    route {
        cidr_block = "0.0.0.0/0"
        gateway_id = aws_internet_gateway.clamp_ig.id
    }

    tags = {
        Name = "clamp_public_rt"
        Team = "clamp"
        Environment = var.environment
    }
}

resource "aws_route_table_association" "clamp_rt_public_assoc" {
    subnet_id = aws_subnet.clamp_public_subnet.id
    route_table_id = aws_route_table.clamp_public_subnet_route_table.id
}

resource "aws_eip" "clamp_nat_gateway_eip" {
    vpc = true

    tags = {
        Name = "clamp_gateway_eip"
        Team = "clamp"
        Environment = var.environment
    }
}

resource "aws_nat_gateway" "clamp_nat_gateway" {
    allocation_id = "aws_eip.clamp_nat_gateway_eip.id"
    subnet_id = "aws_subnet.clamp_public_subnet.id"

    tags = {
        Name = "clamp_nat_gateway"
        Team = "clamp"
        Environment = var.environment
    }
}

resource "aws_route_table" "clamp_private_subnet_route_table" {
  vpc_id = aws_vpc.clamp_vpc.id

  route {
    cidr_block = "0.0.0.0/0"
    nat_gateway_id = "aws_nat_gateway.clamp_nat_gateway.id"
  }

  tags = {
        Name = "clamp_private_subnet_rt"
        Team = "clamp"
        Environment = var.environment
    }
}

resource "aws_route_table_association" "clamp_rt_private_assoc" {
    subnet_id = "aws_subnet.clamp_private_subnet_1.id"
    route_table_id = "aws_route_table.clamp_private_subnet_route_table.id"
}

resource "aws_instance" "clamp_bastion" {
  instance_type = "t2.micro"
  ami = "ami-03d8059563982d7b0" # https://cloud-images.ubuntu.com/locator/ec2/ (Ubuntu)
  subnet_id = aws_subnet.clamp_public_subnet.id
  security_groups = [aws_security_group.clamp_instance_access.id]
  key_name = aws_key_pair.clamp_ssh.key_name
  disable_api_termination = false
  ebs_optimized = false
  root_block_device {
    volume_size = "10"
  }

  tags = {
        Name = "clamp_bastion"
        Team = "clamp"
        Environment = var.environment
    }
}

resource "aws_eip" "clamp_bastion_eip" {
  instance = aws_instance.clamp_bastion.id
  vpc = true

  tags = {
        Name = "clamp_bastion_ip"
        Team = "clamp"
        Environment = var.environment
    }
}

output "clamp_bastion_ip" {
  value = aws_eip.clamp_bastion_eip.public_ip
}