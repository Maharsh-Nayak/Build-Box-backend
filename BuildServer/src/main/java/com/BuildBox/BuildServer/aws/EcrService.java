package com.BuildBox.BuildServer.aws;

import software.amazon.awssdk.services.ecr.EcrClient;
import software.amazon.awssdk.services.ecr.model.*;

import org.springframework.stereotype.Service;

@Service
public class EcrService {

    private final EcrClient ecr = EcrClient.create();

    public String ensureRepository(String repoName) {
        try {
            DescribeRepositoriesResponse response =
                    ecr.describeRepositories(
                            DescribeRepositoriesRequest.builder()
                                    .repositoryNames(repoName)
                                    .build()
                    );
            return response.repositories().get(0).repositoryUri();
        } catch (RepositoryNotFoundException e) {
            CreateRepositoryResponse response =
                    ecr.createRepository(
                            CreateRepositoryRequest.builder()
                                    .repositoryName(repoName)
                                    .build()
                    );
            return response.repository().repositoryUri();
        }
    }
}
