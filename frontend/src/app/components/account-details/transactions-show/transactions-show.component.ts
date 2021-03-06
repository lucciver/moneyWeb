import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { from, Observable } from 'rxjs';
import { TransactionEditComponent } from './transaction-edit/transaction-edit.component';
import { Transaction } from '../../../domain/account/Transaction';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { CategoriesService } from '../../../services/categories.service';
import { filter, first, flatMap, map, reduce } from 'rxjs/operators';
import { MatDialog } from '@angular/material/dialog';
import { ConfirmationDialogComponent } from '../../shared/confirmation-dialog/confirmation-dialog.component';
import { SubCategoryFamily } from '../../../domain/categories/SubCategoryFamily';

@Component({
  selector: 'app-transactions-show',
  templateUrl: './transactions-show.component.html',
  styleUrls: ['./transactions-show.component.css']
})
export class TransactionsShowComponent implements OnInit {

  @Input() transactions$: Observable<Transaction[]>;
  @Input() accountsName: string[];
  @Output() transactionUpdate = new EventEmitter<Transaction>();
  @Output() transactionRemove = new EventEmitter<Transaction>();
  @Output() transactionCreate = new EventEmitter<Transaction>();

  totalDepenses: number;
  totalRevenus: number;

  private categoriesMap: Map<string, SubCategoryFamily> = new Map<string, SubCategoryFamily>();

  constructor(private modalService: NgbModal,
              private categoriesService: CategoriesService,
              public dialog: MatDialog) {
    this.categoriesService.getCategoriesFlatMapAsSubCategories().pipe(
      flatMap(x => x),
    ).subscribe(subCate => this.categoriesMap.set(subCate.id, subCate));
  }

  ngOnInit(): void {
    this.transactions$.pipe(
      first(),
      flatMap(t => t),
      filter(t => !this.isInternalVirement(t)),
      filter(t => t.amount <= 0),
      map(t => t.amount),
      reduce((acc, value) => acc += value, 0),
    ).subscribe(result => this.totalDepenses = result);


  }


  openTransactionEditDialog(transaction: Transaction) {
    const modalRef = this.modalService.open(TransactionEditComponent,
      {backdropClass: 'light-blue-backdrop', size: 'lg'});
    modalRef.componentInstance.transaction = Object.assign({}, transaction);

    from(modalRef.result)
      .subscribe(transactionResult => {
        if (transactionResult != null) {
          this.transactionUpdate.emit(transactionResult);
        }
      });
  }

  copyTransaction(transaction: Transaction) {
    const modalRef = this.modalService.open(TransactionEditComponent,
      {backdropClass: 'light-blue-backdrop', size: 'lg'});
    const newTransaction = Object.assign({}, transaction);
    newTransaction.id = null;
    modalRef.componentInstance.transaction = newTransaction;

    from(modalRef.result)
      .subscribe(transactionResult => {
        if (transactionResult != null) {
          this.transactionCreate.emit(transactionResult);
        }
      });
  }

  deleteTransaction(transaction: Transaction) {
    const dialogRef = this.dialog.open(ConfirmationDialogComponent, {
      width: '350px',
      data: 'Do you confirm the deletion of this transaction ?'
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        console.log('Delete transaction');
        this.transactionRemove.emit(transaction);
      }
    });
  }

  getCategoryName(transaction: Transaction): string {
    if (transaction.ventilations.length === 0) {
      return '';
    }
    if (transaction.ventilations.length > 1) {
      return 'Ventilation';
    }
    if (this.categoriesMap.get(transaction.ventilations[0].categoryId)) {
      return this.categoriesMap.get(transaction.ventilations[0].categoryId).name;
    }
    return null;
  }

  isInternalVirement(transaction: Transaction) {
    const subCategoryFamily = this.categoriesMap.get(transaction.ventilations[0].categoryId);
    if (subCategoryFamily === undefined) {
      return false;
    }
    return subCategoryFamily.isInternalVirement;
  }

  pointTransaction(transaction: Transaction) {
    transaction.isPointe = true;
    this.transactionUpdate.emit(transaction);
  }

  createNewTransaction() {
    const modalRef = this.modalService.open(TransactionEditComponent,
      {backdropClass: 'light-blue-backdrop', size: 'lg'});
    modalRef.componentInstance.transaction = new Transaction(null, 0, '', '', '', false, new Date(), this.accountsName[0], []);

    from(modalRef.result).subscribe(
      transactionResult => {
        if (transactionResult != null) {
          this.transactionCreate.emit(transactionResult);
        }
      });
  }

  getTotalRevenus(transactions$: Observable<Transaction[]>): Observable<number> {
   return transactions$.pipe(
      first(),
      flatMap(t => t),
      filter(t => !this.isInternalVirement(t)),
      filter(t => t.amount > 0),
      map(t => t.amount),
      reduce((acc, value) => acc += value, 0),
    );
  }

  getTotalDepenses(transactions$: Observable<Transaction[]>): Observable<number> {
    return transactions$.pipe(
      first(),
      flatMap(t => t),
      filter(t => !this.isInternalVirement(t)),
      filter(t => t.amount <= 0),
      map(t => t.amount),
      reduce((acc, value) => acc += value, 0),
    );
  }

}
