import java.io.Serializable;
import java.lang.annotation.Inherited;

@Entity
@Table(name = "usuarios")
public class Usuario implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Integer id;
    
    @Column(name = "nome", unique = true, nullable = false)
    private String nome;
}