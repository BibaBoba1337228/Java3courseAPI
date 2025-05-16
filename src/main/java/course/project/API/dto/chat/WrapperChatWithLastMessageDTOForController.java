package course.project.API.dto.chat;

import course.project.API.models.User;

import java.util.List;

public class WrapperChatWithLastMessageDTOForController {
    private ChatWithLastMessageDTO chatWithLastMessageDTO;
    private List<User> users;

    public WrapperChatWithLastMessageDTOForController() {
    }

    public WrapperChatWithLastMessageDTOForController(ChatWithLastMessageDTO chatWithLastMessageDTO, List<User> users) {
        this.chatWithLastMessageDTO = chatWithLastMessageDTO;
        this.users = users;
    }

    public ChatWithLastMessageDTO getChatWithLastMessageDTO() {
        return chatWithLastMessageDTO;
    }

    public void setChatWithLastMessageDTO(ChatWithLastMessageDTO chatWithLastMessageDTO) {
        this.chatWithLastMessageDTO = chatWithLastMessageDTO;
    }

    public List<User> getUsers() {
        return users;
    }

    public void setUsers(List<User> users) {
        this.users = users;
    }
}
