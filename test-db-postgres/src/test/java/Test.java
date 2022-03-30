@SpringBootTest(webEnvironment = WebEnvironment.NONE, classes = { ClientTest.Config.class })
@ExtendWith({ SpringExtension.class })
//@TestMethodOrder(OrderAnnotation.class)
//@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
@ActiveProfiles("test")
public class ClientTest extends ContainerManager {

@SpringBootTest(classes = Application.class)
public class Test {

    @Test
    @Transactional
    public void givenUsersInDB_WhenUpdateStatusForNameModifyingQueryAnnotationNative_ThenModifyMatchingUsers() {
        // same as above
    }
}