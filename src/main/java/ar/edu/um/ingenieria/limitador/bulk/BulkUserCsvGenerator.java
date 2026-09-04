package ar.edu.um.ingenieria.limitador.bulk;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

import net.datafaker.Faker;

/**
 * Genera 1.000.000 de usuarios ficticios en {@code usuarios_bulk.csv}.
 *
 * <p>Distribución exacta de roles: 1 "admin", 5 "manager" y el resto
 * "guest", con las posiciones privilegiadas elegidas al azar a lo largo
 * de todo el archivo.</p>
 *
 * <p>Pensado para máximo rendimiento: escritura secuencial/streamed,
 * agrupada en lotes (batching) sobre un BufferedWriter con buffer grande,
 * sin estructuras en memoria que contengan el millón de objetos a la vez.</p>
 */
public final class BulkUserCsvGenerator {

    private static final long TOTAL = 1_000_000L;
    private static final int ADMIN_COUNT = 1;
    private static final int MANAGER_COUNT = 5;
    private static final int PROGRESS_EVERY = 100_000;
    private static final int BATCH_SIZE = 10_000;
    private static final int IO_BUFFER_BYTES = 16 * 1024 * 1024;
    private static final String OUTPUT = "usuarios_bulk.csv";

    private static final String[] EMAIL_DOMAINS = {
        "example.com", "mail.com", "um.edu.ar", "test.edu.ar", "gmail.com"
    };

    private BulkUserCsvGenerator() {
    }

    public static void main(String[] args) throws IOException {
        long startNanos = System.nanoTime();

        int[] privileged = pickPrivilegedPositions();
        int adminPosition = privileged[0];

        Path output = Paths.get(OUTPUT);
        Faker faker = new Faker(new Locale("es", "AR"));

        try (BufferedWriter writer = new BufferedWriter(
                Files.newBufferedWriter(output,
                        StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE,
                        StandardOpenOption.WRITE,
                        StandardOpenOption.TRUNCATE_EXISTING),
                IO_BUFFER_BYTES)) {

            writer.write("username,email,firstName,lastName,address,phoneNumber,active,role\n");

            StringBuilder batch = new StringBuilder(BATCH_SIZE * 160);
            for (long row = 0; row < TOTAL; row++) {
                String firstName = faker.name().firstName();
                String lastName = faker.name().lastName();
                String base = stripForId(firstName + lastName);
                String username = base + String.format(Locale.ROOT, "%06d", row);
                String email = username + "@" + EMAIL_DOMAINS[faker.random().nextInt(EMAIL_DOMAINS.length)];
                String address = faker.address().fullAddress();
                String phone = faker.phoneNumber().cellPhone();
                boolean active = faker.bool().bool();

                batch.append(username).append(',')
                        .append(email).append(',');
                appendCsvField(batch, firstName);
                batch.append(',');
                appendCsvField(batch, lastName);
                batch.append(',');
                appendCsvField(batch, address);
                batch.append(',')
                        .append(phone).append(',')
                        .append(active).append(',')
                        .append(roleFor(row, adminPosition, privileged)).append('\n');

                long processed = row + 1;
                if (processed % BATCH_SIZE == 0) {
                    writer.write(batch.toString());
                    batch.setLength(0);
                }
                if (processed % PROGRESS_EVERY == 0) {
                    System.out.printf(Locale.ROOT, "Procesados %,d / %,d registros%n", processed, TOTAL);
                }
            }
            if (batch.length() > 0) {
                writer.write(batch.toString());
            }
        }

        double elapsedSeconds = (System.nanoTime() - startNanos) / 1_000_000_000.0;
        System.out.printf(Locale.ROOT,
                "Finalizado: %,d registros en %s (%,d bytes) en %.1f s%n",
                TOTAL, output.toAbsolutePath(), Files.size(output), elapsedSeconds);
    }

    private static int[] pickPrivilegedPositions() {
        return ThreadLocalRandom.current()
                .ints(0, (int) TOTAL)
                .distinct()
                .limit(ADMIN_COUNT + MANAGER_COUNT)
                .toArray();
    }

    private static String roleFor(long row, int adminPosition, int[] privileged) {
        if (row == adminPosition) {
            return "admin";
        }
        for (int i = 1; i < privileged.length; i++) {
            if (privileged[i] == row) {
                return "manager";
            }
        }
        return "guest";
    }

    private static String stripForId(String raw) {
        return raw.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    private static void appendCsvField(StringBuilder sb, String value) {
        if (value.indexOf(',') < 0 && value.indexOf('"') < 0
                && value.indexOf('\n') < 0 && value.indexOf('\r') < 0) {
            sb.append(value);
            return;
        }
        sb.append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '"') {
                sb.append('"');
            }
            sb.append(c);
        }
        sb.append('"');
    }
}