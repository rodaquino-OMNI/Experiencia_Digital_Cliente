package br.com.austa.experiencia.service.integration;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import java.util.Map;
import java.util.HashMap;

/**
 * Java Delegate para integração com Tasy ERP
 *
 * Responsabilidades:
 * - Criar registros de beneficiários
 * - Buscar informações de beneficiários
 * - Atualizar dados cadastrais
 * - Consultar histórico de utilizações
 *
 * Uso no BPMN:
 * <serviceTask id="ServiceTask_BuscarBeneficiario"
 *              name="Buscar Beneficiário"
 *              camunda:delegateExpression="${tasyBeneficiarioService}">
 * </serviceTask>
 */
@Component("tasyBeneficiarioService")
public class TasyBeneficiarioService implements JavaDelegate {

    private static final Logger LOGGER = LoggerFactory.getLogger(TasyBeneficiarioService.class);

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private TasyConfiguration tasyConfig;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String operacao = (String) execution.getVariable("tasyOperacao");

        if (operacao == null) {
            operacao = "buscar"; // Operação padrão
        }

        LOGGER.info("Executando operação Tasy: {} para processo: {}",
                   operacao, execution.getProcessInstanceId());

        try {
            switch (operacao.toLowerCase()) {
                case "criar":
                    criarBeneficiario(execution);
                    break;
                case "buscar":
                    buscarBeneficiario(execution);
                    break;
                case "atualizar":
                    atualizarBeneficiario(execution);
                    break;
                case "historico":
                    consultarHistorico(execution);
                    break;
                default:
                    throw new IllegalArgumentException("Operação inválida: " + operacao);
            }
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            LOGGER.error("Erro na comunicação com Tasy: {}", e.getMessage(), e);
            execution.setVariable("tasyErro", true);
            execution.setVariable("tasyErroMensagem", e.getMessage());
            throw new Exception("ERR_TASY_INDISPONIVEL", e);
        }
    }

    /**
     * Cria um novo registro de beneficiário no Tasy
     *
     * Input:
     * - dadosCadastrais (Map): Dados do beneficiário
     * - contratoId (String): ID do contrato
     *
     * Output:
     * - beneficiarioTasyId (String): ID do beneficiário no Tasy
     */
    private void criarBeneficiario(DelegateExecution execution) {
        @SuppressWarnings("unchecked")
        Map<String, Object> dadosCadastrais = (Map<String, Object>) execution.getVariable("dadosCadastrais");
        String contratoId = (String) execution.getVariable("contratoId");

        LOGGER.info("Criando beneficiário no Tasy - Contrato: {}", contratoId);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("dados_cadastrais", dadosCadastrais);
        requestBody.put("contrato_id", contratoId);

        try {
            String url = tasyConfig.getBaseUrl() + "/api/v1/beneficiarios";

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(url, requestBody, Map.class);

            String beneficiarioTasyId = (String) response.get("beneficiario_id");
            execution.setVariable("beneficiarioTasyId", beneficiarioTasyId);
            execution.setVariable("tasyErro", false);

            LOGGER.info("Beneficiário criado com sucesso no Tasy - ID: {}", beneficiarioTasyId);
        } catch (Exception e) {
            LOGGER.error("Erro ao criar beneficiário no Tasy", e);
            throw e;
        }
    }

    /**
     * Busca informações de um beneficiário no Tasy
     *
     * Input:
     * - beneficiarioId (String) OU telefone (String) OU cpf (String)
     *
     * Output:
     * - beneficiarioEncontrado (Boolean)
     * - beneficiarioTasyId (String)
     * - dadosBeneficiario (Map)
     */
    private void buscarBeneficiario(DelegateExecution execution) {
        String beneficiarioId = (String) execution.getVariable("beneficiarioId");
        String telefone = (String) execution.getVariable("telefone");
        String cpf = (String) execution.getVariable("cpf");

        String parametroBusca = null;
        String valor = null;

        if (beneficiarioId != null) {
            parametroBusca = "id";
            valor = beneficiarioId;
        } else if (cpf != null) {
            parametroBusca = "cpf";
            valor = cpf;
        } else if (telefone != null) {
            parametroBusca = "telefone";
            valor = telefone;
        }

        LOGGER.info("Buscando beneficiário no Tasy por {}: {}", parametroBusca, valor);

        try {
            String url = String.format("%s/api/v1/beneficiarios?%s=%s",
                                      tasyConfig.getBaseUrl(), parametroBusca, valor);

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            if (response != null && response.containsKey("beneficiario")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> beneficiario = (Map<String, Object>) response.get("beneficiario");

                execution.setVariable("beneficiarioEncontrado", true);
                execution.setVariable("beneficiarioTasyId", beneficiario.get("id"));
                execution.setVariable("dadosBeneficiario", beneficiario);

                LOGGER.info("Beneficiário encontrado no Tasy - ID: {}", beneficiario.get("id"));
            } else {
                execution.setVariable("beneficiarioEncontrado", false);
                LOGGER.warn("Beneficiário não encontrado no Tasy");
            }
        } catch (HttpClientErrorException.NotFound e) {
            execution.setVariable("beneficiarioEncontrado", false);
            LOGGER.warn("Beneficiário não encontrado no Tasy: {}", valor);
        } catch (Exception e) {
            LOGGER.error("Erro ao buscar beneficiário no Tasy", e);
            throw e;
        }
    }

    /**
     * Atualiza dados cadastrais de um beneficiário
     *
     * Input:
     * - beneficiarioId (String)
     * - dadosAtualizacao (Map)
     *
     * Output:
     * - atualizacaoSucesso (Boolean)
     */
    private void atualizarBeneficiario(DelegateExecution execution) {
        String beneficiarioId = (String) execution.getVariable("beneficiarioId");
        @SuppressWarnings("unchecked")
        Map<String, Object> dadosAtualizacao = (Map<String, Object>) execution.getVariable("dadosAtualizacao");

        LOGGER.info("Atualizando beneficiário no Tasy - ID: {}", beneficiarioId);

        try {
            String url = String.format("%s/api/v1/beneficiarios/%s",
                                      tasyConfig.getBaseUrl(), beneficiarioId);

            restTemplate.put(url, dadosAtualizacao);

            execution.setVariable("atualizacaoSucesso", true);
            LOGGER.info("Beneficiário atualizado com sucesso no Tasy");
        } catch (Exception e) {
            execution.setVariable("atualizacaoSucesso", false);
            LOGGER.error("Erro ao atualizar beneficiário no Tasy", e);
            throw e;
        }
    }

    /**
     * Consulta histórico de utilizações do beneficiário
     *
     * Input:
     * - beneficiarioId (String)
     * - diasHistorico (Integer): Número de dias de histórico (padrão: 90)
     *
     * Output:
     * - historicoUtilizacoes (List<Map>): Lista de utilizações
     */
    private void consultarHistorico(DelegateExecution execution) {
        String beneficiarioId = (String) execution.getVariable("beneficiarioId");
        Integer diasHistorico = (Integer) execution.getVariable("diasHistorico");

        if (diasHistorico == null) {
            diasHistorico = 90;
        }

        LOGGER.info("Consultando histórico de {} dias para beneficiário: {}",
                   diasHistorico, beneficiarioId);

        try {
            String url = String.format("%s/api/v1/beneficiarios/%s/historico?dias=%d",
                                      tasyConfig.getBaseUrl(), beneficiarioId, diasHistorico);

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            execution.setVariable("historicoUtilizacoes", response.get("utilizacoes"));

            LOGGER.info("Histórico consultado com sucesso");
        } catch (Exception e) {
            LOGGER.error("Erro ao consultar histórico no Tasy", e);
            throw e;
        }
    }
}

/**
 * Classe de configuração para acesso ao Tasy
 */
@Component
class TasyConfiguration {

    @Value("${tasy.api.base-url}")
    private String baseUrl;

    @Value("${tasy.api.timeout:5000}")
    private Integer timeout;

    public String getBaseUrl() {
        return baseUrl;
    }

    public Integer getTimeout() {
        return timeout;
    }
}
