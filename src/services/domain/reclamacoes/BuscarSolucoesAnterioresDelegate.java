package br.com.austa.experiencia.services.domain.reclamacoes;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Busca soluções aplicadas em reclamações similares anteriores.
 *
 * Variáveis de entrada:
 * - tipoReclamacao (String)
 * - causaRaizIdentificada (String)
 * - categoriaCausaRaiz (String)
 *
 * Variáveis de saída:
 * - solucoesAnteriores (List<SolucaoDTO>)
 * - solucaoRecomendada (SolucaoDTO)
 * - taxaSucessoSolucao (Double)
 */
@Slf4j
@Component("buscarSolucoesAnterioresDelegate")
@RequiredArgsConstructor
public class BuscarSolucoesAnterioresDelegate implements JavaDelegate {

    private final ReclamacaoService reclamacaoService;
    private final KnowledgeBaseService knowledgeBase;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String tipoReclamacao = (String) execution.getVariable("tipoReclamacao");
        String causaRaiz = (String) execution.getVariable("causaRaizIdentificada");

        // 1. Buscar soluções no histórico
        List<SolucaoDTO> solucoesHistorico = reclamacaoService
            .buscarSolucoesPorTipoECausa(tipoReclamacao, causaRaiz);

        // 2. Buscar na base de conhecimento
        List<SolucaoDTO> solucoesKB = knowledgeBase
            .searchSolutions(tipoReclamacao, causaRaiz);

        // 3. Consolidar e rankear por taxa de sucesso
        List<SolucaoDTO> todasSolucoes = Stream
            .concat(solucoesHistorico.stream(), solucoesKB.stream())
            .distinct()
            .sorted(Comparator.comparing(SolucaoDTO::getTaxaSucesso).reversed())
            .collect(Collectors.toList());

        // 4. Selecionar melhor solução
        SolucaoDTO melhorSolucao = todasSolucoes.isEmpty() ? null : todasSolucoes.get(0);

        // 5. Definir variáveis
        execution.setVariable("solucoesAnteriores", todasSolucoes);
        execution.setVariable("solucaoRecomendada", melhorSolucao);
        execution.setVariable("taxaSucessoSolucao",
            melhorSolucao != null ? melhorSolucao.getTaxaSucesso() : 0.0);
        execution.setVariable("existeSolucaoConhecida", !todasSolucoes.isEmpty());

        log.info("Encontradas {} soluções anteriores. Melhor taxa de sucesso: {}%",
                 todasSolucoes.size(),
                 melhorSolucao != null ? melhorSolucao.getTaxaSucesso() * 100 : 0);
    }
}
