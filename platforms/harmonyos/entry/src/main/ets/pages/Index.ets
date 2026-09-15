/**
 * DepMap HarmonyOS 原生适配器编译验证页。
 *
 * 本页不承载产品 UI（产品 UI 在 app/ 的 uni-app x 工程内），
 * 仅用于让 entry 模块成为一个可独立编译的最小 HAP 载体。
 */
@Entry
@Component
struct Index {
  @State message: string = '个人数字依赖图';
  @State note: string = 'HarmonyOS 原生适配器编译验证工程';

  build() {
    Column({ space: 12 }) {
      Text(this.message)
        .fontSize(26)
        .fontWeight(FontWeight.Bold)
        .fontColor('#1F2333')
      Text(this.note)
        .fontSize(14)
        .fontColor('#8A90A6')
    }
    .width('100%')
    .height('100%')
    .justifyContent(FlexAlign.Center)
    .backgroundColor('#F5F6FA')
  }
}
