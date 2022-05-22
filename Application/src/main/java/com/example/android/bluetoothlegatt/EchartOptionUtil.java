package com.example.android.bluetoothlegatt;

import com.github.abel533.echarts.axis.CategoryAxis;
import com.github.abel533.echarts.axis.ValueAxis;
import com.github.abel533.echarts.code.Trigger;
import com.github.abel533.echarts.json.GsonOption;
import com.github.abel533.echarts.series.Line;

public class EchartOptionUtil {
    /**
     * 画折线图
     *
     * @param xAxis x轴的相关配置
     * @param yAxis y轴的相关配置
     * @return
     */
    public static GsonOption getLineChartOptions(Object[] xAxis, Object[] yAxis) {
        //通过option指定图表的配置项和数
        GsonOption option = new GsonOption();
        option.title("折线图");//折线图的标题
        option.legend("预测值");//添加图例
        option.tooltip().trigger(Trigger.axis);//提示框（详见tooltip），鼠标悬浮交互时的信息提示

        ValueAxis valueAxis = new ValueAxis();
        option.yAxis(valueAxis);//添加y轴

        CategoryAxis categorxAxis = new CategoryAxis();
        categorxAxis.axisLine().onZero(false);//坐标轴线，默认显示，属性show控制显示与否，属性lineStyle（详见lineStyle）控制线条样式
        categorxAxis.boundaryGap(true);
        categorxAxis.data(xAxis);//添加坐标轴的类目属性
        option.xAxis(categorxAxis);//x轴为类目轴

        Line line = new Line();

        //设置折线的相关属性
        line.smooth(true).name("预测值").data(yAxis).itemStyle().normal().lineStyle().shadowColor("rgba(0,0,0,0.4)");

        //添加数据，将数据添加到option中
        option.series(line);
        return option;
    }

}
