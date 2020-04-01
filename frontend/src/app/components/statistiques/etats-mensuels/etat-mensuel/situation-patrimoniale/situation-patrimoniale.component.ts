import { Component, Input, OnInit } from '@angular/core';
import { filter, flatMap, map, toArray } from 'rxjs/operators';
import { StatistiquesService } from '../../../../../services/statistiques.service';

@Component({
  selector: 'app-situation-patrimoniale',
  templateUrl: './situation-patrimoniale.component.html',
  styleUrls: ['./situation-patrimoniale.component.css']
})
export class SituationPatrimonialeComponent implements OnInit {

  @Input() currentDate: Date;
  chartDatas: any[];
  colorScheme = {domain: ['#9CD27D']  };

  constructor(private statistiquesService: StatistiquesService) {
  }

  ngOnInit() {
    this.refreshDatas();
  }

  refreshDatas() {
    let solde = 0;
    const lastYear = new Date(this.currentDate.getFullYear() - 1, this.currentDate.getMonth(), 1);
    const lastYearTime = lastYear.getTime();
    const currentYearTime = this.currentDate.getTime();

    this.statistiquesService.getMonthStatsAggregateByMonth(true).pipe(
      flatMap(t => t),
      map(account => {
        solde += account.revenus - account.depenses;
        return new SoldeMonth(account.getMonthTime(), solde);
      }),
      filter(account => account.monthTime >= lastYearTime),
      filter(account => account.monthTime <= currentYearTime),
      toArray()
    ).subscribe(
      accountMonthStats => {
        this.chartDatas = [...[]];
        accountMonthStats.forEach(accountMonthStat => {
          const chartDatasCurrent = {
            name: new Date(accountMonthStat.monthTime),
            value: accountMonthStat.solde,
          };
          this.chartDatas = [...this.chartDatas, chartDatasCurrent];
        });
      }
    );
  }

}

class SoldeMonth {
  monthTime: number;
  solde: number;

  constructor(month: number, solde: number) {
    this.monthTime = month;
    this.solde = solde;
  }
}
