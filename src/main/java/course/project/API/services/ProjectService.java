package course.project.API.services;

import course.project.API.dto.project.ProjectDTO;
import course.project.API.dto.project.ProjectResponse;
import course.project.API.models.Project;
import course.project.API.models.User;
import course.project.API.repositories.ProjectRepository;
import course.project.API.repositories.UserRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ProjectService {
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final ModelMapper modelMapper;

    @Autowired
    public ProjectService(ProjectRepository projectRepository, UserRepository userRepository, ModelMapper modelMapper) {
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.modelMapper = modelMapper;
    }

    public List<ProjectDTO> getAllProjects() {
        return projectRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public Optional<ProjectDTO> getProjectById(Long id) {
        return projectRepository.findById(id)
                .map(this::convertToDTO);
    }

    @Transactional
    public ProjectDTO createProject(ProjectDTO projectDTO, String username) {
        User owner = userRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("User not found"));
        Project project = new Project(projectDTO.getTitle(), projectDTO.getDescription());
        project.setOwner(owner);
        if (projectDTO.getParticipants() != null) {
            Set<User> participants = new HashSet<>();
            for (String uname : projectDTO.getParticipants()) {
                userRepository.findByUsername(uname).ifPresent(participants::add);
            }
            project.setParticipants(participants);
        }
        Project savedProject = projectRepository.save(project);
        return convertToDTO(savedProject);
    }

    @Transactional
    public Optional<ProjectDTO> updateProject(Long id, ProjectDTO projectDTO) {
        return projectRepository.findById(id)
                .map(project -> {
                    project.setTitle(projectDTO.getTitle());
                    project.setDescription(projectDTO.getDescription());
                    if (projectDTO.getParticipants() != null) {
                        Set<User> participants = new HashSet<>();
                        for (String uname : projectDTO.getParticipants()) {
                            userRepository.findByUsername(uname).ifPresent(participants::add);
                        }
                        project.setParticipants(participants);
                    }
                    return convertToDTO(projectRepository.save(project));
                });
    }

    @Transactional
    public void deleteProject(Long id) {
        projectRepository.deleteById(id);
    }

    @Transactional
    public Optional<ProjectDTO> addParticipant(Long projectId, Long userId) {
        return projectRepository.findById(projectId)
                .flatMap(project -> userRepository.findById(userId)
                        .map(user -> {
                            project.addParticipant(user);
                            return convertToDTO(projectRepository.save(project));
                        }));
    }

    @Transactional
    public Optional<ProjectDTO> removeParticipant(Long projectId, Long userId) {
        return projectRepository.findById(projectId)
                .flatMap(project -> userRepository.findById(userId)
                        .map(user -> {
                            project.removeParticipant(user);
                            return convertToDTO(projectRepository.save(project));
                        }));
    }

    public List<ProjectDTO> getMyProjects(Long userId) {
        return projectRepository.findByOwner_IdOrParticipants_Id(userId, userId)
            .stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    public List<ProjectResponse> getMyProjectsWithUsers(Long userId) {
        return projectRepository.findByOwner_IdOrParticipants_Id(userId, userId)
            .stream().map(this::convertToResponseDTO).collect(Collectors.toList());
    }

    private ProjectDTO convertToDTO(Project project) {
        Set<String> participants = project.getParticipants().stream()
                .map(User::getUsername)
                .collect(Collectors.toSet());
        
        ProjectDTO projectDTO = modelMapper.map(project, ProjectDTO.class);
        projectDTO.setParticipants(participants);
        return projectDTO;
    }

    private ProjectResponse convertToResponseDTO(Project project) {
        return modelMapper.map(project, ProjectResponse.class);
    }
} 