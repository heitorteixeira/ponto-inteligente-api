package com.heitor.pontointeligente.api.controllers;

import java.math.BigDecimal;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;

import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.heitor.pontointeligente.api.dtos.CadastroPessoaFisicaDTO;
import com.heitor.pontointeligente.api.entities.Empresa;
import com.heitor.pontointeligente.api.entities.Funcionario;
import com.heitor.pontointeligente.api.enums.PerfilEnum;
import com.heitor.pontointeligente.api.response.Response;
import com.heitor.pontointeligente.api.services.EmpresaService;
import com.heitor.pontointeligente.api.services.FuncionarioService;
import com.heitor.pontointeligente.api.utils.PasswordUtils;

@RestController
@RequestMapping("/api/cadastrar-pf")
@CrossOrigin(origins = "*")
public class CadastroPFController {

private static final Logger log = LoggerFactory.getLogger(CadastroPFController.class);
    
    @Autowired
    private EmpresaService empresaService;
    
    @Autowired
    private FuncionarioService funcionarioService;

    public CadastroPFController() {
    }

    /**
     * Cadastra um funcionário pessoa física no sistema.
     * 
     * @param cadastroPFDto
     * @param result
     * @return ResponseEntity<Response<CadastroPFDto>>
     * @throws NoSuchAlgorithmException
     */
    @PostMapping
    public ResponseEntity<Response<CadastroPessoaFisicaDTO>> cadastrar(@Valid @RequestBody CadastroPessoaFisicaDTO cadastroPFDto,
            BindingResult result) throws NoSuchAlgorithmException {
        log.info("Cadastrando PF: {}", cadastroPFDto.toString());
        Response<CadastroPessoaFisicaDTO> response = new Response<CadastroPessoaFisicaDTO>();

        validarDadosExistentes(cadastroPFDto, result);
        Funcionario funcionario = this.converterDtoParaFuncionario(cadastroPFDto, result);

        if (result.hasErrors()) {
            log.error("Erro validando dados de cadastro PF: {}", result.getAllErrors());
            result.getAllErrors().forEach(error -> response.getErrors().add(error.getDefaultMessage()));
            return ResponseEntity.badRequest().body(response);
        }
        
        Optional<Empresa> empresa = this.empresaService.buscarPorCnpj(cadastroPFDto.getCnpj());
        empresa.ifPresent(emp -> funcionario.setEmpresa(emp));
        this.funcionarioService.persistir(funcionario);

        response.setData(this.converterCadastroPFDto(funcionario));
        return ResponseEntity.ok(response);
    }

    /**
     * Verifica se a empresa está cadastrada e se o funcionário não existe na base de dados.
     * 
     * @param cadastroPFDto
     * @param result
     */
    private void validarDadosExistentes(CadastroPessoaFisicaDTO cadastroPFDto, BindingResult result) {
        Optional<Empresa> empresa = this.empresaService.buscarPorCnpj(cadastroPFDto.getCnpj());
        if (!empresa.isPresent()) {
            result.addError(new ObjectError("empresa", "Empresa não cadastrada."));
        }
        
        this.funcionarioService.buscarPorCpf(cadastroPFDto.getCpf())
            .ifPresent(func -> result.addError(new ObjectError("funcionario", "CPF já existente.")));

        this.funcionarioService.buscarPorEmail(cadastroPFDto.getEmail())
            .ifPresent(func -> result.addError(new ObjectError("funcionario", "Email já existente.")));
    }

    /**
     * Converte os dados do DTO para funcionário.
     * 
     * @param cadastroPFDto
     * @param result
     * @return Funcionario
     * @throws NoSuchAlgorithmException
     */
    private Funcionario converterDtoParaFuncionario(CadastroPessoaFisicaDTO cadastroPFDto, BindingResult result)
            throws NoSuchAlgorithmException {
        Funcionario funcionario = new Funcionario();
        funcionario.setNome(cadastroPFDto.getNome());
        funcionario.setEmail(cadastroPFDto.getEmail());
        funcionario.setCpf(cadastroPFDto.getCpf());
        funcionario.setPerfil(PerfilEnum.ROLE_USUARIO);
        funcionario.setSenha(PasswordUtils.gerarBCrypt(cadastroPFDto.getSenha()));
        cadastroPFDto.getQtdHorasAlmoco()
                .ifPresent(qtdHorasAlmoco -> funcionario.setQtdHorasAlmoco(Float.valueOf(qtdHorasAlmoco)));
        cadastroPFDto.getQtdHorasTrabalhoDia()
                .ifPresent(qtdHorasTrabDia -> funcionario.setQtdHorasTrabalhoDia(Float.valueOf(qtdHorasTrabDia)));
        cadastroPFDto.getValorHora().ifPresent(valorHora -> funcionario.setValorHora(new BigDecimal(valorHora)));

        return funcionario;
    }

    /**
     * Popula o DTO de cadastro com os dados do funcionário e empresa.
     * 
     * @param funcionario
     * @return CadastroPFDto
     */
    private CadastroPessoaFisicaDTO converterCadastroPFDto(Funcionario funcionario) {
        CadastroPessoaFisicaDTO cadastroPFDto = new CadastroPessoaFisicaDTO();
        cadastroPFDto.setId(funcionario.getId());
        cadastroPFDto.setNome(funcionario.getNome());
        cadastroPFDto.setEmail(funcionario.getEmail());
        cadastroPFDto.setCpf(funcionario.getCpf());
        cadastroPFDto.setCnpj(funcionario.getEmpresa().getCnpj());
        funcionario.getQtdHorasAlmocoOpt().ifPresent(qtdHorasAlmoco -> cadastroPFDto
                .setQtdHorasAlmoco(Optional.of(Float.toString(qtdHorasAlmoco))));
        funcionario.getQtdHorasTrabalhoDiaOpt().ifPresent(
                qtdHorasTrabDia -> cadastroPFDto.setQtdHorasTrabalhoDia(Optional.of(Float.toString(qtdHorasTrabDia))));
        funcionario.getValorHoraOpt()
                .ifPresent(valorHora -> cadastroPFDto.setValorHora(Optional.of(valorHora.toString())));

        return cadastroPFDto;
    }
    
}
