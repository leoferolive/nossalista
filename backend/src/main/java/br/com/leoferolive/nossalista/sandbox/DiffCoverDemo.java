/*
 * ARQUIVO DE DEMONSTRACAO DESCARTAVEL — NAO MERGEAR.
 *
 * Existe apenas para validar AO VIVO o gate de cobertura diferencial
 * (diff-cover --fail-under=80) no CI. Nao possui testes de proposito:
 * as linhas/branches abaixo devem aparecer como NAO cobertas no diff do PR,
 * fazendo o step "Diff coverage gate" falhar como esperado. Remover apos a
 * observacao.
 */
package br.com.leoferolive.nossalista.sandbox;

/**
 * Classe descartavel usada apenas para exercitar o gate diff-cover.
 */
public final class DiffCoverDemo {

    private DiffCoverDemo() {
    }

    /**
     * Classifica um numero como par ou impar.
     *
     * @param value valor de entrada
     * @return rotulo "par" ou "impar"
     */
    public static String parity(int value) {
        if (value % 2 == 0) {
            return "par";
        } else {
            return "impar";
        }
    }

    /**
     * Retorna o maior entre dois inteiros.
     *
     * @param first  primeiro valor
     * @param second segundo valor
     * @return o maior dos dois
     */
    public static int max(int first, int second) {
        if (first >= second) {
            return first;
        } else {
            return second;
        }
    }
}
