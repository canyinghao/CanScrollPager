# CanScrollPager

继承RecyclerView，在上下滑动中，支持指定页面有viewpager的切换效果，划过指定页面，能否正常滑动。

适用与某些item需要撑满屏幕，并且完全展示的场景。划过此item，又能正常流畅滚动。

![](./pic/CanScrollPager.gif)

## 添加依赖
```
compile 'com.github.canyinghao:CanScrollPager:1.0.0'
```

## 使用方法
```
scrollPager.setSnapHelper(ScrollPagerSnapHelper())
scrollPager.setOnScrollPageChangedListener
```
如果要哪一个item支持viewpager式滑动效果，需要给此itemView设置tag=true


### 开发者

![](https://avatars3.githubusercontent.com/u/12572840?v=3&s=460)

canyinghao:

<canyinghao@hotmail.com>




### License

    Copyright 2023 canyinghao

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.