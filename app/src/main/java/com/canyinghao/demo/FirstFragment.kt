package com.canyinghao.demo

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.canyinghao.canadapter.CanHolderHelper
import com.canyinghao.canadapter.CanRVAdapter
import com.canyinghao.canscrollpgaer.OnScrollPagerListener
import com.canyinghao.canscrollpgaer.ScrollPagerSnapHelper
import com.canyinghao.demo.databinding.FragmentFirstBinding

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class FirstFragment : Fragment() {

    private var _binding: FragmentFirstBinding? = null


    private val binding get() = _binding!!
    private val model = Model()

    private val colors = arrayListOf(
        "#002375",
        "#00C1B6",
        "#9C6B00",
        "#730000",
        "#006446",
        "#666666",
        "#51C0FF",
        "#FC6976"
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentFirstBinding.inflate(inflater, container, false)


        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.model = model
        model.title.set("hello world")
        model.btnName.set("click")
        binding.buttonFirst.setOnClickListener {
            model.btnName.set("goToSecond")
            findNavController().navigate(R.id.action_FirstFragment_to_SecondFragment)
        }

        binding.scrollPager.setSnapHelper(ScrollPagerSnapHelper())
        val adapter =
            object : CanRVAdapter<String>(binding.scrollPager, R.layout.item_scroll_pager) {
                override fun setView(helper: CanHolderHelper, position: Int, bean: String) {
                    val isSupport =  position == 5 || position == 6
                    helper.convertView.tag = isSupport
                    helper.setText(R.id.tv_index, bean)
                    val index = position % colors.size
                    helper.setText(R.id.tv_desc, if (isSupport) "支持ViewPager效果" else "不支持")
                    helper.convertView.setBackgroundColor(Color.parseColor(colors[index]))
                }

                override fun setItemListener(helper: CanHolderHelper?) {

                }


            }
        binding.scrollPager.setOnScrollPageChangedListener(object : OnScrollPagerListener {


            override fun onPageRelease(isNext: Boolean, position: Int, view: View?) {
                model.title.set("onPageRelease position=${position}")
                Log.d("test", model.title.get()!!)
            }

            override fun onPageSelected(position: Int, bottom: Boolean, view: View?) {
                model.title.set("onPageSelected position=${position}")
                Log.d("test", model.title.get()!!)
            }


        })
        binding.scrollPager.adapter = adapter



        for (i in 0..10) {
            adapter.addLastItem(i.toString())
        }


    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}