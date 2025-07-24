import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table

@Table("files")
data class File(
    @Id val id: Long? = null,
    val fileId: String,
    val tags: List<String> = emptyList()
)
