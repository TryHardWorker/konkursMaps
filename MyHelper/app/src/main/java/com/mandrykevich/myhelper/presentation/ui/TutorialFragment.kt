package com.mandrykevich.myhelper.presentation.ui

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.mandrykevich.myhelper.R
import com.mandrykevich.myhelper.databinding.FragmentTutorialBinding
import com.mandrykevich.myhelper.utils.Constants.MAIN

class TutorialFragment : Fragment() {
    lateinit var binding: FragmentTutorialBinding



    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentTutorialBinding.inflate(inflater)



        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val pages = arrayOf(
            "Вас приветствует приложение MyHelper, приложение создано для населения с ограниченными возможностями, что поможет оставлять зданиям комментарии.",
            "Оставляйте комментарии чтобы и другие пользователи могли узнать о здании подробнее!",
            "В создании комментариев есть распространенные опции обустройства зданий для людей с ограниченными возможностями, такие как: Асистент, Заезд для колясок, Парковка.",
            "В приложении есть функция поиска по названию, которая перенесет на середину экрана искоемое здание.",
            "На вкладке аккаунта вы можете увидеть свои уже оставленные комментарии.",
            "Так же вы можете выйти с аккаунта после нажатия на соответствующую кнопку и подтвеждение."
        )

        val images = arrayOf(
            R.drawable.page_f,
            R.drawable.page_s,
            R.drawable.page_t,
            R.drawable.page_4,
            R.drawable.page_5,
            R.drawable.page_6
        )
        var currentPage = 0

        binding.ivTutorial.setImageResource(images[currentPage])
        binding.tvTutorial.text = pages[currentPage]

        binding.btnNext.setOnClickListener {
            currentPage++

            if (currentPage < pages.size) {
                binding.ivTutorial.setImageResource(images[currentPage])
                binding.tvTutorial.text = pages[currentPage]
            }

            if (binding.tvTutorial.text == pages[5]) {
                binding.btnNext.text = "Закрыть"
                MAIN.binding.bNav.selectedItemId = R.id.mapFragment
            }

            if (currentPage >= pages.size) {
                MAIN.navController.navigate(R.id.action_tutorialFragment_to_mapFragment)
                MAIN.binding.bNav.visibility =View.VISIBLE
            }
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = TutorialFragment()
    }
}