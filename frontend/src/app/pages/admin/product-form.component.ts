import { Component, OnDestroy, OnInit, computed, inject, input, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { Category, Product, ProductImage } from '../../core/models';
import { CategoryApi, ProductApi, UploadApi } from '../../core/services/api.services';
import { AuthService } from '../../core/services/auth.service';
import { errorMessage } from '../../core/utils';
import { ToastService } from '../../shared/toast.component';

/** Imagem ainda não enviada: arquivo local (vai para o Cloudinary) ou URL externa. */
interface PendingImage {
  key: number;
  preview: string;
  file?: File;
  url?: string;
  main: boolean;
}

/** Imagens enviadas pelo AdminUploadController ficam nesta pasta do Cloudinary. */
const CLOUDINARY_FOLDER = 'lira-semijoias/';

@Component({
  selector: 'app-product-form',
  imports: [ReactiveFormsModule, RouterLink],
  template: `
    <div class="page-header">
      <div>
        <a routerLink="/admin/produtos" class="small">← Produtos</a>
        <h1>{{ isEdit() ? 'Editar produto' : 'Adicionar produto' }}</h1>
      </div>
      @if (product(); as p) {
        @if (p.active) { <a [href]="'/produto/' + p.slug" target="_blank" class="btn outline sm">Ver na loja ↗</a> }
      }
    </div>

    @if (loadError()) {
      <div class="alert">{{ loadError() }}</div>
    } @else {
      <form [formGroup]="form" (ngSubmit)="save()" class="layout">
        <div class="col">
          <section class="card">
            <h3>Informações</h3>
            <div class="form-grid">
              <div class="field full">
                <label for="name">Nome *</label>
                <input id="name" formControlName="name" placeholder="Ex.: Brinco Argola Cravejada" />
              </div>
              <div class="field">
                <label for="sku">SKU (código) *</label>
                <input id="sku" formControlName="sku" placeholder="Ex.: BRI-001" />
                <span class="hint">Único por produto. Salvo em maiúsculas.</span>
              </div>
              <div class="field">
                <label for="category">Categoria *</label>
                <select id="category" formControlName="categoryId">
                  <option value="" disabled>Selecione...</option>
                  @for (c of categories(); track c.id) {
                    <option [value]="c.id">{{ c.name }}{{ c.active ? '' : ' (inativa)' }}</option>
                  }
                </select>
                @if (auth.isAdmin()) {
                  @if (!creatingCategory()) {
                    <button type="button" class="link" (click)="creatingCategory.set(true)">+ Nova categoria</button>
                  } @else {
                    <div class="row inline-cat">
                      <input #catName placeholder="Nome da categoria" (keydown.enter)="$event.preventDefault(); createCategory(catName.value)" />
                      <button type="button" class="btn sm" (click)="createCategory(catName.value)">Criar</button>
                      <button type="button" class="btn sm ghost" (click)="creatingCategory.set(false)">✕</button>
                    </div>
                  }
                } @else if (categories().length === 0) {
                  <span class="hint">Nenhuma categoria ativa. Peça a um administrador para criar.</span>
                }
              </div>
              <div class="field full">
                <label for="description">Descrição *</label>
                <textarea id="description" formControlName="description" placeholder="Detalhes da peça, medidas, banho, garantia..."></textarea>
              </div>
              <div class="field">
                <label for="material">Material</label>
                <input id="material" formControlName="material" placeholder="Ex.: Banho de ouro 18k" />
              </div>
              <div class="field">
                <label for="color">Cor</label>
                <input id="color" formControlName="color" placeholder="Ex.: Dourado" />
              </div>
            </div>
          </section>

          <section class="card">
            <h3>Imagens</h3>
            <p class="muted small">A imagem marcada como <strong>principal</strong> aparece na vitrine. Arquivos são enviados ao Cloudinary ao salvar.</p>

            <div class="images">
              @for (img of savedImages(); track img.publicId) {
                <div class="img-box" [class.main]="img.main">
                  <img [src]="img.url" alt="" />
                  <div class="img-actions">
                    @if (!img.main) { <button type="button" (click)="setSavedMain(img)">Principal</button> }
                    @else { <span>Principal</span> }
                    <button type="button" class="del" (click)="removeSaved(img)">Remover</button>
                  </div>
                </div>
              }
              @for (img of pending(); track img.key) {
                <div class="img-box pending" [class.main]="img.main">
                  <img [src]="img.preview" alt="" />
                  <span class="tag">Novo</span>
                  <div class="img-actions">
                    @if (!img.main) { <button type="button" (click)="setPendingMain(img)">Principal</button> }
                    @else { <span>Principal</span> }
                    <button type="button" class="del" (click)="removePending(img)">Remover</button>
                  </div>
                </div>
              }

              <label class="drop" [class.over]="dragOver()"
                     (dragover)="$event.preventDefault(); dragOver.set(true)"
                     (dragleave)="dragOver.set(false)"
                     (drop)="onDrop($event)">
                <input type="file" accept="image/*" multiple (change)="onFiles($event)" />
                <span class="plus">+</span>
                <span class="small">Adicionar fotos</span>
                <span class="hint">Arraste ou clique (até 10MB)</span>
              </label>
            </div>

            <div class="row mt url-row">
              <input #imageUrl type="url" placeholder="...ou cole a URL de uma imagem (https://...)" />
              <button type="button" class="btn outline sm" (click)="addUrl(imageUrl.value); imageUrl.value = ''">Adicionar URL</button>
            </div>
          </section>
        </div>

        <div class="col side">
          <section class="card">
            <h3>Preço</h3>
            <div class="field">
              <label for="price">Preço (R$) *</label>
              <input id="price" type="number" step="0.01" min="0.01" formControlName="price" />
            </div>
            <div class="field mt">
              <label for="promo">Preço promocional (R$)</label>
              <input id="promo" type="number" step="0.01" min="0.01" formControlName="promotionalPrice" />
              <span class="hint">Deixe vazio se não houver promoção. Deve ser menor que o preço.</span>
            </div>
          </section>

          <section class="card">
            <h3>Estoque</h3>
            @if (!isEdit()) {
              <div class="field">
                <label for="stock">Quantidade inicial *</label>
                <input id="stock" type="number" min="0" step="1" formControlName="stock" />
              </div>
            } @else {
              <p>Estoque atual: <strong>{{ product()?.stock }}</strong></p>
              <a routerLink="/admin/estoque" [queryParams]="{ produto: product()?.id }" class="small">Registrar entrada/saída →</a>
            }
            <div class="field mt">
              <label for="min">Estoque mínimo (alerta)</label>
              <input id="min" type="number" min="0" step="1" formControlName="minimumStock" />
            </div>
          </section>

          <section class="card">
            <h3>Visibilidade</h3>
            <label class="checkbox"><input type="checkbox" formControlName="featured" /> Destaque na página inicial</label>
            @if (isEdit()) {
              <label class="checkbox mt"><input type="checkbox" formControlName="active" /> Produto ativo (visível na loja)</label>
            }
          </section>

          @if (error()) { <div class="alert">{{ error() }}</div> }

          <button type="submit" class="btn block" [disabled]="saving()">
            {{ saving() ? savingLabel() : isEdit() ? 'Salvar alterações' : 'Cadastrar produto' }}
          </button>
          @if (!isEdit()) {
            <label class="checkbox small another"><input type="checkbox" #another [checked]="addAnother()" (change)="addAnother.set(another.checked)" /> Cadastrar outro em seguida</label>
          }
        </div>
      </form>
    }
  `,
  styles: `
    .layout { display: grid; grid-template-columns: 1fr 320px; gap: 1.5rem; align-items: start; }
    .col { display: flex; flex-direction: column; gap: 1.25rem; }
    .side { position: sticky; top: 80px; }
    .link { background: none; border: none; color: var(--gold-dark); padding: 0; font: inherit; font-size: 0.82rem; cursor: pointer; text-align: left; }
    .inline-cat { flex-wrap: nowrap; gap: 0.4rem; }
    .images { display: grid; grid-template-columns: repeat(auto-fill, minmax(130px, 1fr)); gap: 0.75rem; }
    .img-box { position: relative; border: 2px solid var(--line); border-radius: 8px; overflow: hidden; aspect-ratio: 1; background: #fff; }
    .img-box.main { border-color: var(--gold); }
    .img-box img { width: 100%; height: 100%; object-fit: cover; }
    .img-box .tag { position: absolute; top: 6px; left: 6px; background: var(--ink); color: #fff; font-size: 0.68rem; padding: 0.1rem 0.45rem; border-radius: 999px; }
    .img-actions {
      position: absolute;
      inset: auto 0 0 0;
      display: flex;
      justify-content: space-between;
      background: rgba(43, 36, 32, 0.75);
      padding: 0.25rem 0.4rem;
      font-size: 0.72rem;
      color: #fff;
    }
    .img-actions button { background: none; border: none; color: #fff; font: inherit; cursor: pointer; padding: 0; }
    .img-actions .del { color: #ffb4ab; }
    .drop {
      border: 2px dashed var(--line);
      border-radius: 8px;
      aspect-ratio: 1;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      gap: 0.2rem;
      cursor: pointer;
      color: var(--muted);
      text-align: center;
      padding: 0.5rem;
    }
    .drop:hover, .drop.over { border-color: var(--gold); background: var(--gold-soft); }
    .drop input { display: none; }
    .drop .plus { font-size: 2rem; color: var(--gold); line-height: 1; }
    .drop .hint { font-size: 0.7rem; }
    .url-row { flex-wrap: nowrap; }
    .another { margin-top: 0.75rem; justify-content: center; }
    @media (max-width: 900px) { .layout { grid-template-columns: 1fr; } .side { position: static; } }
  `,
})
export class ProductFormComponent implements OnInit, OnDestroy {
  /** Parâmetro de rota :id (ausente em /admin/produtos/novo). */
  readonly id = input<string>();

  private productApi = inject(ProductApi);
  private categoryApi = inject(CategoryApi);
  private uploadApi = inject(UploadApi);
  private toast = inject(ToastService);
  private router = inject(Router);
  private fb = inject(FormBuilder);
  protected auth = inject(AuthService);

  protected isEdit = computed(() => !!this.id());
  protected product = signal<Product | null>(null);
  protected categories = signal<Category[]>([]);
  protected savedImages = signal<ProductImage[]>([]);
  protected pending = signal<PendingImage[]>([]);
  protected saving = signal(false);
  protected savingLabel = signal('Salvando...');
  protected error = signal('');
  protected loadError = signal('');
  protected creatingCategory = signal(false);
  protected dragOver = signal(false);
  protected addAnother = signal(false);
  private nextKey = 1;

  protected form = this.fb.group({
    name: ['', Validators.required],
    sku: ['', Validators.required],
    description: ['', Validators.required],
    price: this.fb.control<number | null>(null, [Validators.required, Validators.min(0.01)]),
    promotionalPrice: this.fb.control<number | null>(null, Validators.min(0.01)),
    categoryId: ['', Validators.required],
    material: [''],
    color: [''],
    stock: this.fb.nonNullable.control(0, [Validators.required, Validators.min(0)]),
    minimumStock: this.fb.nonNullable.control(0, [Validators.min(0)]),
    featured: [false],
    active: [true],
  });

  ngOnInit(): void {
    const categories$ = this.auth.isAdmin() ? this.categoryApi.adminList() : this.categoryApi.list();
    categories$.subscribe({ next: (list) => this.categories.set(list) });

    const id = this.id();
    if (id) {
      this.productApi.adminList().subscribe({
        next: (list) => {
          const p = list.find((x) => x.id === id);
          if (!p) {
            this.loadError.set('Produto não encontrado.');
            return;
          }
          this.fill(p);
        },
        error: (err) => this.loadError.set(errorMessage(err)),
      });
    }
  }

  ngOnDestroy(): void {
    this.pending().forEach((p) => p.file && URL.revokeObjectURL(p.preview));
  }

  // ---------------- Imagens ----------------

  onFiles(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.addFiles(Array.from(input.files ?? []));
    input.value = '';
  }

  onDrop(event: DragEvent): void {
    event.preventDefault();
    this.dragOver.set(false);
    this.addFiles(Array.from(event.dataTransfer?.files ?? []));
  }

  private addFiles(files: File[]): void {
    for (const file of files) {
      if (!file.type.startsWith('image/')) {
        this.toast.error(`${file.name}: apenas imagens são permitidas.`);
        continue;
      }
      if (file.size > 10 * 1024 * 1024) {
        this.toast.error(`${file.name}: arquivo maior que 10MB.`);
        continue;
      }
      this.pushPending({ file, preview: URL.createObjectURL(file) });
    }
  }

  addUrl(url: string): void {
    const value = url.trim();
    if (!/^https?:\/\//i.test(value)) {
      this.toast.error('Informe uma URL válida começando com http:// ou https://');
      return;
    }
    this.pushPending({ url: value, preview: value });
  }

  private pushPending(img: Omit<PendingImage, 'key' | 'main'>): void {
    const hasMain = this.savedImages().some((i) => i.main) || this.pending().some((i) => i.main);
    this.pending.update((list) => [...list, { ...img, key: this.nextKey++, main: !hasMain }]);
  }

  setPendingMain(img: PendingImage): void {
    this.pending.update((list) => list.map((i) => ({ ...i, main: i.key === img.key })));
    this.savedImages.update((list) => list.map((i) => ({ ...i, main: false })));
  }

  removePending(img: PendingImage): void {
    if (img.file) URL.revokeObjectURL(img.preview);
    this.pending.update((list) => {
      const rest = list.filter((i) => i.key !== img.key);
      const anyMain = rest.some((i) => i.main) || this.savedImages().some((i) => i.main);
      return !anyMain && rest.length ? rest.map((i, idx) => ({ ...i, main: idx === 0 })) : rest;
    });
  }

  /** Remove a imagem do produto e, se ela estiver no Cloudinary, apaga o arquivo. */
  async removeSaved(img: ProductImage): Promise<void> {
    const p = this.product();
    if (!p || !confirm('Remover esta imagem do produto?')) return;
    try {
      const updated = await firstValueFrom(this.productApi.removeImage(p.id, img.publicId));
      if (img.publicId.startsWith(CLOUDINARY_FOLDER)) {
        this.uploadApi.deleteImage(img.publicId).subscribe({ error: () => undefined });
      }
      this.product.set(updated);
      this.savedImages.set(updated.images);
      this.toast.success('Imagem removida.');
    } catch (err) {
      this.toast.error(errorMessage(err));
    }
  }

  /** O back end não tem endpoint de "definir principal": removemos e adicionamos de novo com main=true. */
  async setSavedMain(img: ProductImage): Promise<void> {
    const p = this.product();
    if (!p) return;
    try {
      await firstValueFrom(this.productApi.removeImage(p.id, img.publicId));
      const updated = await firstValueFrom(
        this.productApi.addImage(p.id, { url: img.url, publicId: img.publicId, main: true }),
      );
      this.product.set(updated);
      this.savedImages.set(updated.images);
      this.pending.update((list) => list.map((i) => ({ ...i, main: false })));
    } catch (err) {
      this.toast.error(errorMessage(err));
    }
  }

  // ---------------- Categoria rápida ----------------

  createCategory(name: string): void {
    const value = name.trim();
    if (!value) return;
    this.categoryApi.create({ name: value }).subscribe({
      next: (category) => {
        this.categories.update((list) => [...list, category]);
        this.form.controls.categoryId.setValue(category.id);
        this.creatingCategory.set(false);
        this.toast.success(`Categoria "${category.name}" criada.`);
      },
      error: (err) => this.toast.error(errorMessage(err)),
    });
  }

  // ---------------- Salvar ----------------

  async save(): Promise<void> {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.error.set('Preencha os campos obrigatórios (*).');
      return;
    }
    const v = this.form.getRawValue();
    const promotionalPrice = v.promotionalPrice || null;
    if (promotionalPrice && promotionalPrice >= (v.price ?? 0)) {
      this.error.set('O preço promocional deve ser menor que o preço.');
      return;
    }

    this.error.set('');
    this.saving.set(true);
    this.savingLabel.set('Salvando produto...');

    const base = {
      name: v.name!.trim(),
      sku: v.sku!.trim(),
      description: v.description!.trim(),
      price: v.price!,
      promotionalPrice,
      categoryId: v.categoryId!,
      material: v.material?.trim() || undefined,
      color: v.color?.trim() || undefined,
      minimumStock: v.minimumStock ?? 0,
      featured: !!v.featured,
    };

    try {
      const id = this.id();
      let product = id
        ? await firstValueFrom(this.productApi.update(id, { ...base, active: !!v.active }))
        : await firstValueFrom(this.productApi.create({ ...base, stock: v.stock ?? 0 }));

      product = await this.uploadPending(product);

      this.toast.success(id ? 'Produto atualizado!' : 'Produto cadastrado!');
      if (!id && this.addAnother()) {
        this.resetForNext();
      } else if (!id) {
        this.router.navigate(['/admin/produtos', product.id], { replaceUrl: true });
      } else {
        this.fill(product);
      }
    } catch (err) {
      this.error.set(errorMessage(err));
    } finally {
      this.saving.set(false);
    }
  }

  /** Envia as imagens pendentes (Cloudinary → POST /admin/products/{id}/images). */
  private async uploadPending(product: Product): Promise<Product> {
    const queue = this.pending();
    for (let i = 0; i < queue.length; i++) {
      const img = queue[i];
      this.savingLabel.set(`Enviando imagem ${i + 1} de ${queue.length}...`);
      try {
        const uploaded = img.file
          ? await firstValueFrom(this.uploadApi.uploadImage(img.file))
          : { url: img.url!, publicId: `url-${Date.now()}-${img.key}` };
        product = await firstValueFrom(
          this.productApi.addImage(product.id, { url: uploaded.url, publicId: uploaded.publicId, main: img.main }),
        );
        if (img.file) URL.revokeObjectURL(img.preview);
        this.pending.update((list) => list.filter((x) => x.key !== img.key));
      } catch (err) {
        this.toast.error(`Falha ao enviar imagem: ${errorMessage(err)}`);
      }
    }
    return product;
  }

  private fill(p: Product): void {
    this.product.set(p);
    this.savedImages.set(p.images ?? []);
    this.form.patchValue({
      name: p.name,
      sku: p.sku,
      description: p.description,
      price: p.price,
      promotionalPrice: p.promotionalPrice ?? null,
      categoryId: p.categoryId,
      material: p.material ?? '',
      color: p.color ?? '',
      stock: p.stock,
      minimumStock: p.minimumStock,
      featured: p.featured,
      active: p.active,
    });
  }

  private resetForNext(): void {
    const categoryId = this.form.controls.categoryId.value;
    this.form.reset({ stock: 0, minimumStock: 0, featured: false, active: true, categoryId });
    this.pending.set([]);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }
}
