package com.example.alquiler_de_vehiculos.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import com.example.alquiler_de_vehiculos.config.EmpresaProperties;

import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final String remitente;
    private final EmpresaProperties empresa;
    private final String urlFrontend;

    public EmailService(
            @Autowired(required = false) JavaMailSender mailSender,
            @Value("${MAIL_FROM:${spring.mail.username:}}") String remitente,
            EmpresaProperties empresa,
            @Value("${app.frontend-url:http://localhost:4200}") String urlFrontend) {
        this.mailSender = mailSender;
        this.remitente = remitente;
        this.empresa = empresa;
        this.urlFrontend = urlFrontend;
    }

    public boolean enviarCodigoVerificacion(String destinatario, String codigo) {
        if (mailSender == null || remitente == null || remitente.isBlank()) {
            System.out.println(">>> [EmailService] JavaMailSender no está configurado con credenciales.");
            return false;
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(remitente, "Paul Car's Alquiler de Vehículos");
            helper.setTo(destinatario);
            helper.setSubject("Código de Verificación: " + codigo + " - Paul Car's");

            String contenidoHtml = """
                <div style="font-family: 'Segoe UI', Helvetica, Arial, sans-serif; max-width: 540px; margin: 0 auto; padding: 24px; border: 1px solid #e2e8f0; border-radius: 12px; background: #ffffff;">
                    <div style="text-align: center; margin-bottom: 24px;">
                        <h2 style="color: #0d1b3e; margin: 0; font-size: 24px;">Paul Car's</h2>
                        <span style="color: #64748b; font-size: 13px;">Alquiler de Vehículos en Lima, Perú</span>
                    </div>
                    <div style="background: #f8fafc; border-radius: 8px; padding: 20px; margin-bottom: 20px; border-left: 4px solid #0d1b3e;">
                        <h3 style="margin-top: 0; color: #0d1b3e; font-size: 18px;">Recuperación de Contraseña</h3>
                        <p style="color: #475569; font-size: 14px; line-height: 1.5; margin-bottom: 16px;">
                            Has solicitado restablecer la contraseña de tu cuenta. Utiliza el siguiente código de verificación de 6 dígitos para continuar:
                        </p>
                        <div style="text-align: center; margin: 24px 0;">
                            <span style="display: inline-block; background: #0d1b3e; color: #ffffff; font-size: 32px; font-weight: 800; letter-spacing: 8px; padding: 12px 28px; border-radius: 8px;">
                                %s
                            </span>
                        </div>
                        <p style="color: #64748b; font-size: 12px; text-align: center; margin: 0;">
                            Este código es de un solo uso y vencerá en <strong>15 minutos</strong>.
                        </p>
                    </div>
                    <p style="color: #94a3b8; font-size: 12px; line-height: 1.4; margin: 0;">
                        Si tú no solicitaste este cambio, puedes ignorar este correo de forma segura. Tu contraseña actual no se modificará.
                    </p>
                </div>
            """.formatted(codigo);

            helper.setText(contenidoHtml, true);
            mailSender.send(message);
            
            return true;
        } catch (Exception e) {
            System.err.println(">>> [EmailService] Error al enviar correo a " + destinatario + ": " + e.getMessage());
            return false;
        }
    }

    public boolean enviarRechazoReserva(String destinatario, String nombreCliente, String vehiculo,
                                        String motivoLegible, String observaciones) {
        String cuerpo = parrafo("Estimado/a <strong>" + escaparHtml(nombreCliente) + "</strong>, revisamos tu solicitud de reserva y "
                        + "lamentamos informarte que <strong>no pudo ser aprobada</strong>.")
                + tabla(new String[] { "Vehículo solicitado", vehiculo },
                        new String[] { "Motivo", motivoLegible, "destacada" });
        if (observaciones != null && !observaciones.isBlank()) {
            cuerpo += aviso("<strong>Observación del asesor:</strong> " + escaparHtml(observaciones));
        }
        cuerpo += parrafo("<span style=\"color:#64748b;font-size:13px;\">Si necesitas más detalle o quieres presentar documentación complementaria, "
                + "comunícate con nuestra central de atención o acércate a cualquiera de nuestras sedes.</span>");
        String html = plantilla("SOLICITUD NO APROBADA", ROJO, "Actualización de tu solicitud de reserva", cuerpo,
                "Ver mis reservas", "/cliente/reservas");
        return enviarHtml(destinatario, "Actualización de tu solicitud de reserva - Paul Car's", html);
    }

    public boolean enviarHtml(String destinatario, String asunto, String html) {
        if (mailSender == null || remitente == null || remitente.isBlank()
                || destinatario == null || destinatario.isBlank()) {
            return false;
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(remitente, "Paul Car's Alquiler de Vehículos");
            helper.setTo(destinatario);
            helper.setSubject(asunto);
            helper.setText(html, true);
            mailSender.send(message);
            return true;
        } catch (Exception e) {
            System.err.println(">>> [EmailService] Error al enviar correo a " + destinatario + ": " + e.getMessage());
            return false;
        }
    }

    public boolean enviarConAdjunto(String destinatario, String asunto, String html,
                                    String nombreArchivo, byte[] contenido, String tipoMime) {
        if (mailSender == null || remitente == null || remitente.isBlank()
                || destinatario == null || destinatario.isBlank()) {
            System.out.println(">>> [EmailService] No se envió '" + asunto + "': correo no configurado o destinatario vacío.");
            return false;
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(remitente, "Paul Car's Alquiler de Vehículos");
            helper.setTo(destinatario);
            helper.setSubject(asunto);
            helper.setText(html, true);
            helper.addAttachment(nombreArchivo, new org.springframework.core.io.ByteArrayResource(contenido), tipoMime);
            mailSender.send(message);
            return true;
        } catch (Exception e) {
            System.err.println(">>> [EmailService] Error al enviar correo con adjunto a " + destinatario + ": " + e.getMessage());
            return false;
        }
    }


    // ───────────────────────── Plantilla común de los correos al cliente ─────────────────────────

    public static final String VERDE = "verde";
    public static final String ROJO = "rojo";
    public static final String AZUL = "azul";

    /**
     * Arma un correo con la imagen de la marca: encabezado, etiqueta de estado, título, contenido, botón opcional y pie con
     * los datos de la empresa. Los textos que vienen del usuario deben pasarse antes por {@link #escapar(String)}.
     */
    public String plantilla(String etiqueta, String tono, String titulo, String contenidoHtml, String botonTexto, String botonRuta) {
        String fondo = "#dbeafe", texto = "#1e3a8a";
        if (VERDE.equals(tono)) { fondo = "#dcfce7"; texto = "#166534"; }
        if (ROJO.equals(tono)) { fondo = "#fee2e2"; texto = "#991b1b"; }
        String fuente = "font-family:'Segoe UI',Helvetica,Arial,sans-serif;";
        StringBuilder h = new StringBuilder();
        h.append("<div style=\"margin:0;padding:24px 12px;background:#f1f5f9;").append(fuente).append("\">");
        h.append("<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"max-width:600px;margin:0 auto;background:#ffffff;border-radius:12px;overflow:hidden;border:1px solid #e2e8f0;\">");

        // Encabezado con la marca
        h.append("<tr><td style=\"background:#0d1b3e;padding:22px 28px;\">")
         .append("<div style=\"").append(fuente).append("font-size:22px;font-weight:800;letter-spacing:2px;color:#ffffff;\">")
         .append(escaparHtml(empresa.getNombreComercial().toUpperCase())).append("</div>")
         .append("<div style=\"").append(fuente).append("font-size:11px;letter-spacing:3px;color:#c9a227;margin-top:4px;\">ALQUILER DE VEHÍCULOS</div>")
         .append("</td></tr>");
        h.append("<tr><td style=\"height:3px;background:#c9a227;line-height:3px;font-size:0;\">&nbsp;</td></tr>");

        // Cuerpo
        h.append("<tr><td style=\"padding:28px 28px 8px;").append(fuente).append("color:#334155;font-size:14px;line-height:1.6;\">");
        h.append("<span style=\"display:inline-block;background:").append(fondo).append(";color:").append(texto)
         .append(";font-size:11px;font-weight:700;letter-spacing:1px;padding:4px 10px;border-radius:999px;\">")
         .append(escaparHtml(etiqueta)).append("</span>");
        h.append("<h1 style=\"margin:14px 0 14px;font-size:22px;line-height:1.3;color:#0d1b3e;\">").append(escaparHtml(titulo)).append("</h1>");
        h.append(contenidoHtml);
        h.append("</td></tr>");

        // Botón
        if (botonTexto != null && botonRuta != null && urlFrontend != null && !urlFrontend.isBlank()) {
            h.append("<tr><td style=\"padding:8px 28px 24px;\" align=\"left\">")
             .append("<a href=\"").append(escaparHtml(urlFrontend.replaceAll("/+$", "") + botonRuta)).append("\" ")
             .append("style=\"display:inline-block;background:#0d1b3e;color:#ffffff;text-decoration:none;font-weight:700;font-size:14px;padding:12px 26px;border-radius:8px;")
             .append(fuente).append("\">").append(escaparHtml(botonTexto)).append("</a></td></tr>");
        }

        // Pie con los datos de la empresa
        h.append("<tr><td style=\"background:#f8fafc;border-top:1px solid #e2e8f0;padding:18px 28px;").append(fuente)
         .append("font-size:12px;line-height:1.6;color:#64748b;\">")
         .append("<strong style=\"color:#334155;\">").append(escaparHtml(empresa.getRazonSocial())).append("</strong><br>");
        if (empresa.getRuc() != null && !empresa.getRuc().isBlank()) {
            h.append("RUC ").append(escaparHtml(empresa.getRuc())).append(" &middot; ");
        }
        h.append(escaparHtml(empresa.getDireccion())).append("<br>")
         .append(escaparHtml(empresa.getTelefono())).append(" &middot; ").append(escaparHtml(empresa.getEmail()))
         .append(" &middot; ").append(escaparHtml(empresa.getWeb())).append("<br>")
         .append("<span style=\"color:#94a3b8;\">Este es un mensaje automático. Si necesitas ayuda, comunícate con nuestra central de atención.</span>")
         .append("</td></tr></table></div>");
        return h.toString();
    }

    /** Tabla de detalle: cada fila es {etiqueta, valor}; si trae un tercer elemento, la fila se resalta. */
    public String tabla(String[]... filas) {
        StringBuilder t = new StringBuilder("<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"margin:14px 0;border:1px solid #e2e8f0;border-radius:8px;border-collapse:separate;overflow:hidden;\">");
        for (String[] f : filas) {
            boolean resaltada = f.length > 2;
            t.append("<tr><td style=\"padding:10px 14px;font-size:13px;color:#64748b;border-bottom:1px solid #f1f5f9;")
             .append(resaltada ? "background:#f8fafc;" : "").append("\">").append(escaparHtml(f[0])).append("</td>")
             .append("<td align=\"right\" style=\"padding:10px 14px;font-size:14px;font-weight:").append(resaltada ? "800" : "600")
             .append(";color:#0d1b3e;border-bottom:1px solid #f1f5f9;").append(resaltada ? "background:#f8fafc;" : "").append("\">")
             .append(escaparHtml(f[1])).append("</td></tr>");
        }
        return t.append("</table>").toString();
    }

    /** Recuadro destacado para un aviso importante (el texto puede llevar HTML propio, ya escapado). */
    public String aviso(String html) {
        return "<div style=\"margin:14px 0;padding:12px 14px;background:#eff6ff;border-left:4px solid #1d4ed8;border-radius:6px;font-size:13px;line-height:1.55;color:#1e3a8a;\">"
                + html + "</div>";
    }

    public String parrafo(String html) {
        return "<p style=\"margin:0 0 12px;\">" + html + "</p>";
    }

    public String escapar(String texto) {
        return escaparHtml(texto);
    }

    private String escaparHtml(String texto) {
        if (texto == null) return "";
        return texto.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
